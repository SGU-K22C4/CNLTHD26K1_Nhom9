package com.fashion.orderservice.service;

import com.fashion.orderservice.config.VNPayConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    /**
     * Build the full VNPay redirect URL for a given order.
     *
     * @param orderId    The order's database ID (used as vnp_TxnRef)
     * @param totalAmount Total in VND (integer, e.g. 1299000)
     * @param orderInfo  Description shown on VNPay payment page
     * @param ipAddress  The client's IP address
     * @return Full HTTPS URL to redirect the user to VNPay
     */
    public String createPaymentUrl(Long orderId, long totalAmount, String orderInfo, String ipAddress) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(orderId); // unique per transaction
        // VNPay requires amount * 100 (no decimals)
        String vnp_Amount = String.valueOf(totalAmount * 100);
        String vnp_CurrCode = "VND";
        String vnp_Locale = "vn";
        String vnp_OrderType = "other";

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnp_Version);
        params.put("vnp_Command", vnp_Command);
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", vnp_Amount);
        params.put("vnp_CurrCode", vnp_CurrCode);
        params.put("vnp_TxnRef", vnp_TxnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnp_OrderType);
        params.put("vnp_Locale", vnp_Locale);
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress);

        // VNPay requires Vietnamese timezone (UTC+7)
        // NOTE: "Etc/GMT+7" is actually UTC-7 in POSIX — use "Asia/Ho_Chi_Minh" instead
        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(tz);
        Calendar cal = Calendar.getInstance(tz);
        String vnp_CreateDate = formatter.format(cal.getTime());
        params.put("vnp_CreateDate", vnp_CreateDate);

        cal.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cal.getTime());
        params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build query string and hash
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<Map.Entry<String, String>> itr = params.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                     .append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        return vnPayConfig.getPaymentUrl() + "?" + query.toString();
    }

    /**
     * Validate the response parameters from VNPay callback by checking the secure hash.
     *
     * @param params All query parameters from VNPay redirect URL
     * @return true if signature is valid
     */
    public boolean validateSignature(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null) return false;

        // Remove hash fields before re-computing
        Map<String, String> fields = new TreeMap<>(params);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> itr = fields.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, String> entry = itr.next();
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String computedHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return computedHash.equalsIgnoreCase(vnp_SecureHash);
    }
}
