# GraphWalker MBT

Thu muc nay chua model MBT (Model-Based Testing) cho luong dang ky khoa hoc.

## File model

- `course-registration-flow.graphml`

## Cac trang thai/chuyen trang chinh trong model

- Guest vao trang courses
- Login thanh cong
- Student dang ky khoa hoc
- Admin approve/reject
- Student hoc va hoan thanh
- Certificate duoc cap (neu khoa hoc bat cert)

## Cach dung nhanh

1. Mo file graphml trong GraphWalker Studio.
2. Sinh duong di voi random/path generator.
3. Mapping edge/vertex sang test step implementation trong framework test cua ban.

Vi du command (tham khao, tuy phien ban GraphWalker):

```bash
graphwalker offline --model tests/graphwalker/course-registration-flow.graphml "random(edge_coverage(100))"
```
