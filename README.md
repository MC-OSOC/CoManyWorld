# co-many-world

co-many-world เป็นปลั๊กอินสำหรับ Minecraft 1.21ขี้นไป ที่ช่วยให้คุณสามารถสร้างและจัดการหลายโลก
## คุณสมบัติ

- สร้างโลกใหม่ได้ง่ายดายด้วยคำสั่ง
- วาร์ปไปยังโลกต่าง ๆ ได้อย่างรวดเร็ว
- ลบโลกที่ไม่ต้องการ
- รายชื่อโลกที่สร้างไว้
- รองรับ EssentialsX

## การติดตั้ง

1. ดาวน์โหลดไฟล์ จาก release
2. นำไฟล์ .jar ไปไว้ในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์ Minecraft

## คำสั่ง

#### สร้างโลกใหม่
`/co-many create <worldName> [-11|-12|-13]`
- `-11` : โลกปกติ (ค่าเริ่มต้น)
- `-12` : โลกนรก (Nether)
- `-13` : โลก The End
- `-all`: สร้างโลกทั่งหมด

#### วาร์ปไปยังโลก
`/co-many tp <worldName>`
#### รายชื่อโลก
`/co-many list`
#### ลบโลก (ปิดใช้งานโลก)
`/co-many del <worldName>`
#### นำเข้าโลก
`/co-many import [worldName]`

## การบันทึกโลก
- โลกถูกสร้างโดยปลั๊กอินนี้ จะถูกบันทึก รูปแบบ `/world_many_custom_world` โฟลเดอร์ปกติ โลกเดียว ยกเว้น Wrold ของเดิมของ server
- โลกทั้งหมดจะถูกบันทึก หากเป็นชนิต `-all` จะปันทึกในรูปแบบ
- ```text
  custom_world/
  ├──world_many_custom_world/
  ├──world_many_custom_world_nether/
  ├──world_many_custom_world_the_end/
## Permissions

- `co.many.worlds.admin` : สิทธิ์ในการจัดการโลก (สร้าง, วาร์ป, รายชื่อโลก)
- `co.many.worlds.admindel` : สิทธิ์ในการลบโลก

## การตั้งค่า

ไฟล์ `config.yml` จะถูกสร้างขึ้นเมื่อคุณเริ่มเซิร์ฟเวอร์ครั้งแรก คุณสามารถปรับการตั้งค่าได้ดังนี้:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
worlds: []  # List of worlds to be loaded on startup
