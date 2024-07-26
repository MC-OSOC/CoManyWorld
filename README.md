# co-many-world
![ข้อความอธิบายภาพ](backgrounds-01.png)

co-many-world เป็นปลั๊กอินสำหรับ Minecraft 1.21ขี้นไป ที่ช่วยให้คุณสามารถสร้างและจัดการหลายโลก
#### นี้คือปลั๊กอิน ตัวอย่างทดลองใช้งานและทดสอบเท่านั้น ไม่แนะนำให้ไปใช้กับ server จริง เพราะปลั๊กอินนี้อยู่ในช่วงพัฒนา และ อาจจะไม่สมบูรณ์

[English version](https://github.com/MC-OSOC/co-many-world/blob/main/README-EN.md)
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
`/co-many create <worldName> [-11|-12|-13|-all]`
- `-11` : โลกปกติ (ค่าเริ่มต้น)
- `-12` : โลกนรก (Nether)
- `-13` : โลก The End
- `-all`: สร้างโลกทั่งหมด

#### วาร์ปไปยังโลก `/co-many tp <worldName>`
#### รายชื่อโลก `/co-many list`
#### ลบโลก (ลบโลกออกจาก config.yml เท่านั้น) `/co-many delete <worldName>`
#### นำเข้าโลก `/co-many import <worldName>`

## การบันทึกโลก
- โลกถูกสร้างโดยปลั๊กอินนี้ จะถูกบันทึก รูปแบบ `many_world/custom_world/world` โฟลเดอร์ปกติ โลกเดียว ยกเว้น Wrold ของเดิมของ server
-   ```text
    many_world/
    └─custom_world/
      └─world/
- โลกทั้งหมดจะถูกบันทึก หากเป็นชนิต `-all` จะรวมไว้ในโฟลเดอร์เดียวกัน
  ```text
  many_world/
  └─custom_world/
     ├─world/
     ├─world_nether/
     └─world_the_end/
## Permissions

- `co.many.worlds.admin` : สิทธิ์ในการจัดการโลก (สร้าง, วาร์ป, รายชื่อโลก)
- `co.many.worlds.admindel` : สิทธิ์ในการลบโลก

| Permissions             | Command                                                                                         | Properties                                                |
|-------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about | สิทธิ์ในการจัดการโลก (สร้าง, วาร์ป, รายชื่อโลก,นำเข้าโลก) |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | สิทธิ์ในการลบโลก                                          |                                      |


## การตั้งค่า

ไฟล์ `config.yml` จะถูกสร้างขึ้นเมื่อคุณเริ่มเซิร์ฟเวอร์ครั้งแรก คุณสามารถปรับการตั้งค่าได้ดังนี้:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
default-world: 'World'
main-nether-world: 'world_nether'
main-end-world: 'world_the_end'
worlds: []   # List of worlds to be loaded on startup
prevent-single-world-portals: true  # Prevent portal usage in single worlds
```
ไฟล์ `delete_worlds.yml` มีไว้เก็บโลกที่ถูกนำออกจาก `config.yml`
```yaml
delete_worlds: []
```