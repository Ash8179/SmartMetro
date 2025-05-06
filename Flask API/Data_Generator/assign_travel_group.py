import pandas as pd

# 读取 Excel 文件
df = pd.read_excel("/Users/zhangwenyu/Desktop/sh_stat_info.xlsx", dtype=str, engine="openpyxl")

# 打印前几行数据，检查 `name_cn` 是否正确读取
print(df.head())

# 设定 `name_cn` 和 `travel_group` 的列索引
name_cn_col = 2  # `name_cn` 在第三列
travel_group_col = 3  # `travel_group` 在第四列

# 确保 `name_cn` 列没有空值
if df.iloc[:, name_cn_col].isnull().any():
    print("警告：`name_cn` 列包含空值，可能会影响分组，请检查数据！")

# 按 `name_cn` 分组并赋值
df.iloc[:, travel_group_col] = df.groupby(df.iloc[:, name_cn_col], sort=False).ngroup() + 1

# 保存回 Excel
output_file = "/Users/zhangwenyu/Desktop/sh_stat_info_updated.xlsx"
df.to_excel(output_file, index=False, engine="openpyxl")  # 移除 encoding 参数

print("处理完成，结果已保存到 sh_stat_info_updated.xlsx")
