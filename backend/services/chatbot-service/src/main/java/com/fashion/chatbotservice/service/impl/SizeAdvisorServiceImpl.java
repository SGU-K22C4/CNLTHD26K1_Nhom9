package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service thuần: tính toán size thời trang dựa trên số đo cơ thể.
 * Không phụ thuộc LLM — chỉ chứa business logic.
 */
@Service
public class SizeAdvisorServiceImpl implements SizeAdvisorService {

    private static final List<String> SIZE_ORDER = List.of("XS", "S", "M", "L", "XL", "XXL");

    // HEIGHT_CM: yêu cầu từ "cao" đứng trước số đo để tránh nhầm "78cm" (eo) thành chiều cao
    private static final Pattern HEIGHT_CM_WITH_CONTEXT = Pattern.compile("cao\\s+(\\d{3})\\s*(cm|centimet)?", Pattern.CASE_INSENSITIVE);
    // HEIGHT_M: luôn ưu tiên parse trước vì phổ biến nhất trong tiếng Việt (1m65, 1m70...)
    private static final Pattern HEIGHT_M = Pattern.compile("(1)\\s*m\\s*(\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT = Pattern.compile("(\\d{2,3})\\s*(kg|ky|kilo)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHEST = Pattern.compile("(nguc|v1)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAIST = Pattern.compile("(eo|v2)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern WAIST_LOOSE = Pattern.compile("eo[^\\d]{0,20}(\\d{2,3})\\s*(cm)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIP = Pattern.compile("(hong|v3)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIP_LOOSE = Pattern.compile("hong[^\\d]{0,20}(\\d{2,3})\\s*(cm)?", Pattern.CASE_INSENSITIVE);

    @Override
    public Measurements extractMeasurements(String message) {
        String normalized = VietnameseNormalizer.normalize(message);

        // 1) Ưu tiên parse dạng "1m65" trước
        Integer heightCm = null;
        Matcher meterMatcher = HEIGHT_M.matcher(normalized);
        if (meterMatcher.find()) {
            heightCm = Integer.parseInt(meterMatcher.group(1)) * 100
                    + Integer.parseInt(meterMatcher.group(2));
        }
        // 2) Nếu chưa tìm được, thử "cao 165cm" hoặc "cao 165"
        if (heightCm == null) {
            heightCm = extractNumber(HEIGHT_CM_WITH_CONTEXT, normalized, 1);
        }

        // 3) Parse vòng eo: thử strict trước, nếu không thì loose ("eo ... 78cm")
        Integer waistCm = extractNumber(WAIST, normalized, 2);
        if (waistCm == null) {
            waistCm = extractNumber(WAIST_LOOSE, normalized, 1);
        }

        // 4) Parse vòng hông: thử strict trước, nếu không thì loose ("hong ... 95cm")
        Integer hipCm = extractNumber(HIP, normalized, 2);
        if (hipCm == null) {
            hipCm = extractNumber(HIP_LOOSE, normalized, 1);
        }

        return new Measurements(
                heightCm,
                extractNumber(WEIGHT, normalized, 1),
                extractNumber(CHEST, normalized, 2),
                waistCm,
                hipCm
        );
    }

    @Override
    public SizeResult suggest(Measurements m, GarmentType garmentType) {
        int index;

        if (m.weightKg() >= 75 || m.heightCm() >= 178) {
            index = 4; // XL
        } else if (m.weightKg() >= 67 || m.heightCm() >= 172) {
            index = 3; // L
        } else if (m.weightKg() <= 50 || m.heightCm() <= 158) {
            index = 1; // S
        } else {
            index = 2; // M
        }

        if (garmentType == GarmentType.TOP && m.chestCm() != null && m.chestCm() >= 100) {
            index++;
        }

        if (garmentType == GarmentType.BOTTOM) {
            int waist = m.waistCm() != null ? m.waistCm() : 0;
            int hip = m.hipCm() != null ? m.hipCm() : 0;
            if (waist >= 86 || hip >= 102) {
                index++;
            } else if (waist > 0 && waist <= 70 && hip > 0 && hip <= 90) {
                index--;
            }
        }

        index = Math.max(0, Math.min(index, SIZE_ORDER.size() - 1));
        String size = SIZE_ORDER.get(index);

        String note = garmentType == GarmentType.BOTTOM
                ? "Quần/chân váy phụ thuộc nhiều vào eo-hông, nếu thích ôm sát có thể giảm 1 size."
                : "Áo phụ thuộc vòng ngực và vai, nếu thích thoải mái có thể tăng 1 size.";

        return new SizeResult(size, note);
    }

    @Override
    public GarmentType detectGarmentType(String message) {
        String normalized = VietnameseNormalizer.normalize(message);
        if (normalized.contains("quan") || normalized.contains("jean")
                || normalized.contains("chan vay") || normalized.contains("vay")) {
            return GarmentType.BOTTOM;
        }
        return GarmentType.TOP;
    }

    private Integer extractNumber(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;
        try {
            return Integer.parseInt(matcher.group(group));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
