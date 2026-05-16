package com.fashion.chatbotservice.service.impl;

import com.fashion.chatbotservice.model.ChatSession;
import com.fashion.chatbotservice.service.SizeAdvisorService;
import com.fashion.chatbotservice.service.SizeFitAdvisoryService;
import com.fashion.chatbotservice.util.VietnameseNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SizeFitAdvisoryServiceImpl implements SizeFitAdvisoryService {

    private static final List<String> SIZE_ORDER = List.of("XS", "S", "M", "L", "XL", "XXL");

    private final SizeAdvisorService sizeAdvisorService;

    @Override
    public SizeFitAdvice advise(
            SizeAdvisorService.Measurements measurements,
            SizeAdvisorService.GarmentType garmentType,
            String garmentContext,
            ChatSession.PreferenceProfile profile) {
        SizeAdvisorService.SizeResult baseline = sizeAdvisorService.suggest(measurements, garmentType);
        int index = SIZE_ORDER.indexOf(baseline.recommendedSize());
        if (index < 0) {
            index = 2;
        }

        String normalizedContext = VietnameseNormalizer.normalize(garmentContext == null ? "" : garmentContext);
        String fitPreference = profile == null ? "" : VietnameseNormalizer.normalize(profile.getFitPreference());

        String rationale = baseline.note();
        String followUp = "";

        if (garmentType == SizeAdvisorService.GarmentType.TOP) {
            if (containsAny(normalizedContext, "so mi", "shirt", "blouse")) {
                if (valueOrZero(measurements.chestCm()) >= 88 || valueOrZero(profile == null ? null : profile.getLastChestCm()) >= 88) {
                    index += 1;
                    rationale = "Dang ao nay nen uu tien thoai mai o vai va nguc. Neu nguc day hoac vai rong thi len 1 size se de mac hon.";
                } else {
                    rationale = "Ao so mi va blouse nen uu tien form vai va nguc. Neu ban muon mac thong thoang hon thi len 1 size la an toan.";
                }
            } else if (containsAny(normalizedContext, "thun", "tee", "basic tee", "ao phong")) {
                rationale = "Ao thun basic thuong dung size vi vai cotton co do gian nhe va de mac hang ngay.";
            } else if (containsAny(normalizedContext, "len", "knit", "cardigan")) {
                rationale = "Knitwear thuong dung size vi co do co gian tot, khong can len size tru khi ban muon form rong.";
            } else if (containsAny(normalizedContext, "crop")) {
                if (containsAny(fitPreference, "om", "slim", "vua van")) {
                    index -= 1;
                    rationale = "Crop top neu muon fit gon thi co the xuong 1 size, nhung chi nen lam vay khi ban thich form sat nguoi.";
                } else {
                    rationale = "Crop top nen dung size de giu form can doi va de chuyen dong.";
                }
            } else if (containsAny(normalizedContext, "blazer", "trench", "outerwear", "ao khoac")) {
                index += 1;
                rationale = "Blazer va ao khoac nen uu tien vua vai va du khoang trong de layering, nen len 1 size se an toan hon.";
                followUp = "Neu ban biet so do vai hoac hay mac brand khac size nao, minh co the chot size chac hon.";
            }
        } else {
            if (containsAny(normalizedContext, "skinny")) {
                index += (valueOrZero(measurements.hipCm()) >= 97 || valueOrZero(measurements.waistCm()) >= 72) ? 2 : 1;
                rationale = "Jeans skinny thuong hep hon o hong va dui, nen len 1 size se de mac hon. Neu mong va dui day thi co the len 2 size.";
            } else if (containsAny(normalizedContext, "jean", "straight", "wide", "trouser", "quan tay")) {
                if (valueOrZero(measurements.hipCm()) > 92 || valueOrZero(measurements.waistCm()) > 68) {
                    index += 1;
                    rationale = "Quan dang nay nen uu tien eo-hong va do thoai mai o phan dui. Neu hong hoac dui day thi len 1 size se dep hon.";
                } else {
                    rationale = "Quan straight hoac wide leg thuong theo eo la chinh, nhung van can de du khoang o hong va dui de mac dep.";
                }
            } else if (containsAny(normalizedContext, "short")) {
                rationale = "Quan short thuong dung size theo eo, vi phan ong quan rong hon va de mac hon.";
            } else if (containsAny(normalizedContext, "vay", "dam", "dress", "chan vay", "skirt")) {
                rationale = "Vay va dam nen uu tien eo va hong hon la can nang. Hai nguoi cung can nang van co the mac khac size neu ty le co the khac nhau.";
                followUp = "Neu ban cho minh them vong eo hoac vong hong thi minh co the chot size chac hon nua.";
            }
        }

        if (containsAny(fitPreference, "oversize", "rong", "thoai mai", "relaxed")) {
            index += 1;
            rationale = rationale + " Vi ban thich form thoai mai hon, minh nghieng ve size nhich len de mac de chiu va de layering.";
        } else if (containsAny(fitPreference, "om", "slim", "vua van", "fit")) {
            rationale = rationale + " Vi ban thich form gon hon, minh se khong day size len qua muc can thiet.";
        }

        index = Math.max(0, Math.min(index, SIZE_ORDER.size() - 1));
        String finalSize = SIZE_ORDER.get(index);

        if (followUp.isBlank() && containsAny(normalizedContext, "blazer", "dam", "vay", "quan", "jean")) {
            followUp = "Neu ban muon, minh co the doi chieu them theo kieu dang va muc do om/rong ma ban thich.";
        }

        return new SizeFitAdvice(finalSize, rationale, followUp);
    }

    private boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank()) return false;
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
