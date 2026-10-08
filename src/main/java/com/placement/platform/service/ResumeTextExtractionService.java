package com.placement.platform.service;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeTextExtractionService {

    public String extractText(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return "";
        }

        byte[] pdfBytes = file.getBytes();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();

            String text = stripper.getText(document);

            if (text == null) {
                return "";
            }

            return text
                    .replace("\u0000", "")
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .trim();
        }
    }
}