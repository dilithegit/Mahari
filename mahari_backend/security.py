import os
import json
from pathlib import Path
from cryptography.fernet import Fernet

DATA_DIR = Path(__file__).parent / "data"
DATA_DIR.mkdir(exist_ok=True)

KEY_FILE = DATA_DIR / "secret.key"
if not KEY_FILE.exists():
    key = Fernet.generate_key()
    KEY_FILE.write_bytes(key)
else:
    key = KEY_FILE.read_bytes()

cipher = Fernet(key)

def save_encrypted_user_data(device_id: str, data: dict):
    user_file = DATA_DIR / f"{device_id}.enc"
    json_bytes = json.dumps(data).encode('utf-8')
    encrypted_data = cipher.encrypt(json_bytes)
    user_file.write_bytes(encrypted_data)

def load_encrypted_user_data(device_id: str) -> dict:
    user_file = DATA_DIR / f"{device_id}.enc"
    if not user_file.exists():
        return None
    encrypted_data = user_file.read_bytes()
    decrypted_bytes = cipher.decrypt(encrypted_data)
    return json.loads(decrypted_bytes.decode('utf-8'))

def delete_user_data(device_id: str) -> bool:
    user_file = DATA_DIR / f"{device_id}.enc"
    if user_file.exists():
        user_file.unlink()
        return True
    return False
