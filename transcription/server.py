"""Local video URL -> FunASR transcription adapter (loopback only)."""
import ipaddress
import os
from pathlib import Path
import socket
import subprocess
import tempfile
from threading import Lock
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parent
os.environ.setdefault('MODELSCOPE_CACHE', str(ROOT / 'models'))
# 抖音等站点会校验 Cookie。新版 Edge/Chrome 可能启用 App-Bound 加密，
# 因此优先支持标准 Netscape cookies.txt 文件；浏览器直读仅作为可选方案。
COOKIE_FILE = os.getenv('YTDLP_COOKIES_FILE', '').strip()
COOKIE_BROWSER = os.getenv('YTDLP_COOKIES_FROM_BROWSER', '').strip().lower()

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from funasr import AutoModel
import yt_dlp

app = FastAPI()
lock = Lock()
model = None


def load_model():
    global model
    if model is None:
        model = AutoModel(model='paraformer-zh', vad_model='fsmn-vad',
                          punc_model='ct-punc', device='cpu', disable_update=True)
    return model


class TranscriptionRequest(BaseModel):
    url: str


def validate_url(value):
    parsed = urlparse(value)
    if parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username:
        raise ValueError('请提供有效的 http/https 视频链接')
    addresses = socket.getaddrinfo(parsed.hostname, parsed.port or 443)
    if any(not ipaddress.ip_address(item[4][0]).is_global for item in addresses):
        raise ValueError('仅支持公网视频链接')


@app.get('/health')
def health():
    return {'status': 'ok', 'modelLoaded': model is not None}


@app.post('/transcribe')
def transcribe(body: TranscriptionRequest):
    try:
        validate_url(body.url)
    except (ValueError, OSError):
        raise HTTPException(400, '请输入有效的公网视频链接')
    if not lock.acquire(blocking=False):
        raise HTTPException(429, '正在处理另一个视频，请稍后重试')
    try:
        with tempfile.TemporaryDirectory(prefix='funasr-') as directory:
            target = Path(directory)
            download_options = {
                'outtmpl': str(target / 'source.%(ext)s'),
                'format': 'bestaudio/best', 'noplaylist': True,
                'max_filesize': 100 * 1024 * 1024, 'socket_timeout': 20,
                'retries': 1, 'quiet': True,
                'match_filter': yt_dlp.utils.match_filter_func('duration <= 1200'),
            }
            is_douyin = (urlparse(body.url).hostname or '').lower().endswith(('.douyin.com', '.iesdouyin.com'))
            if COOKIE_FILE and not is_douyin:
                cookie_path = Path(COOKIE_FILE).expanduser()
                if not cookie_path.is_file():
                    raise ValueError(f'Cookie 文件不存在：{cookie_path}')
                download_options['cookiefile'] = str(cookie_path)
            elif COOKIE_BROWSER and not is_douyin:
                download_options['cookiesfrombrowser'] = (COOKIE_BROWSER,)
            with yt_dlp.YoutubeDL(download_options) as downloader:
                downloader.download([body.url])
            sources = list(target.glob('source.*'))
            if not sources:
                raise ValueError('视频无法下载或时长超过20分钟')
            audio = target / 'audio.wav'
            subprocess.run(['ffmpeg', '-nostdin', '-y', '-i', str(sources[0]),
                            '-vn', '-ar', '16000', '-ac', '1', '-t', '1200', str(audio)],
                           check=True, capture_output=True, timeout=180)
            result = load_model().generate(input=str(audio), batch_size_s=60)
            text = '\n'.join(item.get('text', '') for item in result).strip()
            if not text:
                raise ValueError('未识别到语音内容')
            return {'text': text}
    except Exception as error:
        print(f'Transcription failed: {error}', flush=True)
        raise HTTPException(422, '视频解析或转写失败，请检查链接是否公开且可下载；部分平台需要登录或不支持解析')
    finally:
        lock.release()


if __name__ == '__main__':
    import uvicorn
    load_model()
    uvicorn.run(app, host='127.0.0.1', port=8091)
