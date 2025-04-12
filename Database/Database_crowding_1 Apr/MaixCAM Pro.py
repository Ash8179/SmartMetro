from maix import camera, display, image, nn, app
import requests
import time

detector = nn.YOLOv5(model="/root/models/yolov5s.mud", dual_buff=True)
cam = camera.Camera(detector.input_width(), detector.input_height(), detector.input_format())
disp = display.Display()

# 设置服务器上传接口 URL
url = "http://172.20.10.4:5002/crowding"  # 使用电脑的 IP 地址


while not app.need_exit():
    img = cam.read()
    objs = detector.detect(img, conf_th=0.5, iou_th=0.45)
    
    # 统计 'person' 类别的对象
    person_count = 0
    for obj in objs:
        if detector.labels[obj.class_id] == 'person':  # 检测是否为 'person' 类别
            person_count += 1
            # 画框和标签
            img.draw_rect(obj.x, obj.y, obj.w, obj.h, color=image.COLOR_RED)
            msg = f'{detector.labels[obj.class_id]}: {obj.score:.2f}'
            img.draw_string(obj.x, obj.y, msg, color=image.COLOR_RED)
    
    # 显示检测结果
    disp.show(img)
    
    # 根据人数判断拥挤度（crowd_level），简单规则：根据人数确定拥挤等级
    if person_count <= 10:
        crowd_level = 0  # 不拥挤
    elif person_count <= 30:
        crowd_level = 1  # 稍拥挤
    else:
        crowd_level = 2  # 拥挤
    
    # 构造上传的数据
    data = {
        "line_id": 1,  # 示例值，实际应用时应根据需求填写
        "line_number": 2,
        "line_carriage": 3,
        "person_num": person_count,
        "timestamp": int(time.time()),  # 当前时间戳
        "crowd_level": crowd_level
    }

    # 上传数据到服务器
    try:
        response = requests.post(url, json=data)
        if response.status_code == 200:
            print("数据上传成功")
        else:
            print(f"上传失败: {response.status_code}")
    except Exception as e:
        print(f"请求失败: {str(e)}")
