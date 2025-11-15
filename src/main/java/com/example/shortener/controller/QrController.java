package com.example.shortener.controller;

import com.example.shortener.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "QR Code", description = "Endpoints for generating QR codes for short URLs")
public class QrController {

    private final QrCodeService qrCodeService;

    @GetMapping(value = "/{shortKey}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQr(@PathVariable("shortKey") String shortKey) throws Exception {
        byte[] qrBytes = qrCodeService.getQrCode(shortKey);
        return ResponseEntity.ok(qrBytes);
    }
}
