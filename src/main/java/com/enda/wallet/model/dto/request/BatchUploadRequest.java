package com.enda.wallet.model.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class BatchUploadRequest {
    private MultipartFile file;
}