import os

class Config:
    # 配置数据库连接信息
    SQLALCHEMY_DATABASE_URI = 'mysql+pymysql://17687:Zz13193515431@localhost/smart_metro'
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    SECRET_KEY = os.urandom(24)  # 用于生成会话密钥
