USE cloudbridge;

ALTER TABLE multipart_meta ADD CONSTRAINT FOREIGN KEY meta_uploads_id(UploadID) REFERENCES multipart_uploads(ID) ON DELETE CASCADE;
ALTER TABLE multipart_parts ADD CONSTRAINT FOREIGN KEY part_uploads_id(UploadID) REFERENCES multipart_uploads(ID) ON DELETE CASCADE;
ALTER TABLE multipart_parts ADD UNIQUE part_uploads_number(UploadId, partNumber);
