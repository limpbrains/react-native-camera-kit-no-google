package com.rncamerakit

enum class CodeFormat(val code: String) {
    CODE_128("code-128"),
    CODE_39("code-39"),
    CODE_93("code-93"),
    CODABAR("codabar"),
    EAN_13("ean-13"),
    EAN_8("ean-8"),
    ITF("itf"),
    UPC_A("upc-a"),
    UPC_E("upc-e"),
    QR("qr"),
    PDF_417("pdf-417"),
    AZTEC("aztec"),
    DATA_MATRIX("data-matrix"),
    UNKNOWN("unknown");

    companion object {
        fun fromName(name: String): CodeFormat? {
            val normalized = name.trim().lowercase()
            return values().firstOrNull { format ->
                format.code == normalized || format.name.lowercase() == normalized
            }
        }
    }
}

fun CodeFormat.toBarcodeType(): Int {
    // QR-only Android implementation; no barcode type mapping required.
    return 0
}
