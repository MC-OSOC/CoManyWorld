# co-many-world
![ข้อความอธิบายภาพ](backgrounds-01.png)

co-many-world เป็นปลั๊กอินสำหรับ Minecraft 1.21 ขึ้นไป ที่ช่วยให้คุณสามารถสร้างและจัดการหลายโลก
#### นี่คือปลั๊กอินตัวอย่างทดลองใช้งานและทดสอบเท่านั้น ไม่แนะนำให้ไปใช้กับ server จริง เพราะปลั๊กอินนี้อยู่ในช่วงพัฒนา และอาจจะไม่สมบูรณ์

[English version](https://github.com/MC-OSOC/co-many-world/blob/main/README-EN.md)
## คุณสมบัติ

- สร้างโลกใหม่ได้ง่ายดายด้วยคำสั่ง
- วาร์ปไปยังโลกต่าง ๆ ได้อย่างรวดเร็ว
- ลบโลกที่ไม่ต้องการ
- แสดงรายชื่อโลกที่สร้างไว้
- นำเข้าโลกจากไฟล์ ZIP หรือโฟลเดอร์
- สำรองข้อมูลโลก
- รองรับ EssentialsX

## การติดตั้ง

1. ดาวน์โหลดไฟล์จาก release
2. นำไฟล์ .jar ไปไว้ในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์ Minecraft

## คำสั่ง

#### สร้างโลกใหม่
`/co-many create <worldName> [-11|-12|-13|-all]`
- `-11` : โลกปกติ (ค่าเริ่มต้น)
- `-12` : โลกนรก (Nether)
- `-13` : โลก The End
- `-all`: สร้างโลกทั้งหมด
- `-s` : Seed

#### คำสั่งพื้นฐาน
- **วาร์ปไปยังโลก**: `/co-many tp <worldName>`
- **แสดงรายชื่อโลก**: `/co-many list`
- **ลบโลก** (ลบโลกออกจาก config.yml เท่านั้น): `/co-many delete <worldName>`
- **นำเข้าโลก**: `/co-many import <worldName>`
- **สำรองข้อมูลโลก**: `/co-many backup <worldName>`
- **ลบโลกถาวร (โลกที่อยู่ในถังขยะ)**: `/co-many-clear`
- **ข้อมูลเกี่ยวกับปลั๊กอิน**: `/co-many about`

## การบันทึกโลก
- โลกที่ถูกสร้างโดยปลั๊กอินนี้จะถูกบันทึกในรูปแบบ `many_world/custom_world/world` ยกเว้นโลกเริ่มต้นของเซิร์ฟเวอร์
  ```text
  many_world/
  └─custom_world/
    └─world/
  ```
- หากสร้างโลกแบบ `-all` จะรวมทุกประเภทโลกไว้ในโฟลเดอร์เดียวกัน
  ```text
  many_world/
  └─custom_world/
     ├─world/
     ├─world_nether/
     └─world_the_end/
  ```

## Permissions

| Permissions             | Command                                                                                         | Properties                                                |
|-------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about <br/> /co-many backup | สิทธิ์ในการจัดการโลก (สร้าง, วาร์ป, รายชื่อโลก, นำเข้าโลก, สำรองข้อมูลโลก) |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | สิทธิ์ในการลบโลก                                          |
| co.many.worlds.adminclear | /co-many-clear                                                                                | สิทธิ์ในการลบโลกถาวร                                       |

## การตั้งค่า

ไฟล์ `config.yml` จะถูกสร้างขึ้นเมื่อคุณเริ่มเซิร์ฟเวอร์ครั้งแรก คุณสามารถปรับการตั้งค่าได้ดังนี้:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
default-world: 'World'
default-nether-world: 'world_nether'
default-end-world: 'world_the_end'
worlds: []   # List of worlds to be loaded on startup
```

ไฟล์ `delete_worlds.yml` มีไว้เก็บรายชื่อโลกที่ถูกนำออกจาก `config.yml` และรอการลบถาวร:
```yaml
delete_worlds: []
```

## การนำเข้าโลก

ปลั๊กอินสามารถนำเข้าโลกจากไฟล์ ZIP หรือโฟลเดอร์ได้ โดยจะค้นหาไฟล์ `level.dat` เพื่อระบุตำแหน่งรากของโลก

## การสำรองข้อมูลโลก

คุณสามารถสำรองข้อมูลโลกได้ด้วยคำสั่ง `/co-many backup <worldName>` ไฟล์สำรองข้อมูลจะถูกเก็บไว้ใน `plugins/co-many-world/many_world_backup`