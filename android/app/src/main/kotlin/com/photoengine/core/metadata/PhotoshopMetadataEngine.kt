package com.photoengine.core.metadata

data class ExifMetadata(
    var cameraMake: String? = "Sony",
    var cameraModel: String? = "ILCE-7RM5",
    var lensModel: String? = "FE 24-70mm F2.8 GM II",
    var focalLengthMm: Float? = 50.0f,
    var apertureFNumber: Float? = 2.8f,
    var exposureTimeSec: String? = "1/250",
    var isoSpeedRating: Int? = 100,
    var exposureBiasEv: Float? = 0.0f,
    var meteringMode: String? = "Multi-segment",
    var flashFired: Boolean = false,
    var whiteBalanceMode: String? = "Auto",
    var dateTimeOriginal: String? = "2026:09:09 14:30:00",
    var gpsLatitude: Double? = 37.7749,
    var gpsLongitude: Double? = -122.4194,
    var gpsAltitudeMeters: Double? = 15.0
)

data class IptcMetadata(
    var headline: String? = "Editorial Studio Shoot",
    var category: String? = "Arts/Culture",
    var supplementalCategories: MutableList<String> = mutableListOf("Portrait", "Studio"),
    var keywords: MutableList<String> = mutableListOf("fashion", "lighting", "retouch", "photoshop", "android"),
    var credit: String? = "Master Photo Agency",
    var source: String? = "Studio Production",
    var copyrightNotice: String? = "© 2026 All Rights Reserved",
    var captionWriter: String? = "Lead Editor",
    var city: String? = "San Francisco",
    var provinceState: String? = "California",
    var country: String? = "United States"
)

data class DocumentInfo(
    var title: String = "Untitled-1",
    var author: String = "Professional Photographer",
    var description: String = "High resolution composited master file",
    var copyrightStatus: String = "Copyrighted Work",
    var rating: Int = 5 // 0 .. 5 stars
)

/**
 * Production-ready Photoshop Metadata Engine.
 * Manages full EXIF, IPTC, Document Info and serializes/deserializes Adobe XMP packets (ISO 16684-1).
 */
class PhotoshopMetadataEngine {

    var docInfo = DocumentInfo()
    var exif = ExifMetadata()
    var iptc = IptcMetadata()

    /**
     * Serializes all metadata into a standard Adobe XMP XML packet (<x:xmpmeta>).
     */
    fun serializeToXmpPacket(): String {
        val sb = StringBuilder()
        sb.append("<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n")
        sb.append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core 7.0-c000\">\n")
        sb.append(" <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n")

        // Dublin Core (dc) schema
        sb.append("  <rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n")
        sb.append("   <dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">${escapeXml(docInfo.title)}</rdf:li></rdf:Alt></dc:title>\n")
        sb.append("   <dc:creator><rdf:Seq><rdf:li>${escapeXml(docInfo.author)}</rdf:li></rdf:Seq></dc:creator>\n")
        sb.append("   <dc:description><rdf:Alt><rdf:li xml:lang=\"x-default\">${escapeXml(docInfo.description)}</rdf:li></rdf:Alt></dc:description>\n")
        sb.append("   <dc:rights><rdf:Alt><rdf:li xml:lang=\"x-default\">${escapeXml(iptc.copyrightNotice ?: "")}</rdf:li></rdf:Alt></dc:rights>\n")
        sb.append("   <dc:subject><rdf:Bag>\n")
        for (kw in iptc.keywords) {
            sb.append("    <rdf:li>${escapeXml(kw)}</rdf:li>\n")
        }
        sb.append("   </rdf:Bag></dc:subject>\n")
        sb.append("  </rdf:Description>\n")

        // Photoshop schema (photoshop)
        sb.append("  <rdf:Description rdf:about=\"\" xmlns:photoshop=\"http://ns.adobe.com/photoshop/1.0/\">\n")
        sb.append("   <photoshop:Headline>${escapeXml(iptc.headline ?: "")}</photoshop:Headline>\n")
        sb.append("   <photoshop:Category>${escapeXml(iptc.category ?: "")}</photoshop:Category>\n")
        sb.append("   <photoshop:Credit>${escapeXml(iptc.credit ?: "")}</photoshop:Credit>\n")
        sb.append("   <photoshop:City>${escapeXml(iptc.city ?: "")}</photoshop:City>\n")
        sb.append("   <photoshop:Country>${escapeXml(iptc.country ?: "")}</photoshop:Country>\n")
        sb.append("  </rdf:Description>\n")

        // EXIF schema (exif)
        sb.append("  <rdf:Description rdf:about=\"\" xmlns:exif=\"http://ns.adobe.com/exif/1.0/\">\n")
        exif.cameraMake?.let { sb.append("   <exif:Make>${escapeXml(it)}</exif:Make>\n") }
        exif.cameraModel?.let { sb.append("   <exif:Model>${escapeXml(it)}</exif:Model>\n") }
        exif.lensModel?.let { sb.append("   <exif:LensModel>${escapeXml(it)}</exif:LensModel>\n") }
        exif.focalLengthMm?.let { sb.append("   <exif:FocalLength>${it}</exif:FocalLength>\n") }
        exif.apertureFNumber?.let { sb.append("   <exif:FNumber>${it}</exif:FNumber>\n") }
        exif.exposureTimeSec?.let { sb.append("   <exif:ExposureTime>${escapeXml(it)}</exif:ExposureTime>\n") }
        exif.isoSpeedRating?.let { sb.append("   <exif:ISOSpeedRatings><rdf:Seq><rdf:li>${it}</rdf:li></rdf:Seq></exif:ISOSpeedRatings>\n") }
        exif.dateTimeOriginal?.let { sb.append("   <exif:DateTimeOriginal>${escapeXml(it)}</exif:DateTimeOriginal>\n") }
        if (exif.gpsLatitude != null && exif.gpsLongitude != null) {
            sb.append("   <exif:GPSLatitude>${exif.gpsLatitude}</exif:GPSLatitude>\n")
            sb.append("   <exif:GPSLongitude>${exif.gpsLongitude}</exif:GPSLongitude>\n")
        }
        sb.append("  </rdf:Description>\n")

        // XMP Basic schema (xmp)
        sb.append("  <rdf:Description rdf:about=\"\" xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\">\n")
        sb.append("   <xmp:Rating>${docInfo.rating}</xmp:Rating>\n")
        sb.append("   <xmp:CreatorTool>Photoshop Engine for Android Kotlin 2026</xmp:CreatorTool>\n")
        sb.append("  </rdf:Description>\n")

        sb.append(" </rdf:RDF>\n")
        sb.append("</x:xmpmeta>\n")
        sb.append("<?xpacket end=\"w\"?>")
        return sb.toString()
    }

    /**
     * Parses simple XMP packet tags back into metadata model.
     */
    fun parseXmpPacket(xmpXml: String) {
        val titleMatch = Regex("<dc:title>.*?<rdf:li.*?>(.*?)</rdf:li>", RegexOption.DOT_MATCHES_ALL).find(xmpXml)
        titleMatch?.groupValues?.getOrNull(1)?.let { docInfo.title = unescapeXml(it) }

        val authorMatch = Regex("<dc:creator>.*?<rdf:li>(.*?)</rdf:li>", RegexOption.DOT_MATCHES_ALL).find(xmpXml)
        authorMatch?.groupValues?.getOrNull(1)?.let { docInfo.author = unescapeXml(it) }

        val descMatch = Regex("<dc:description>.*?<rdf:li.*?>(.*?)</rdf:li>", RegexOption.DOT_MATCHES_ALL).find(xmpXml)
        descMatch?.groupValues?.getOrNull(1)?.let { docInfo.description = unescapeXml(it) }

        val cameraMatch = Regex("<exif:Model>(.*?)</exif:Model>").find(xmpXml)
        cameraMatch?.groupValues?.getOrNull(1)?.let { exif.cameraModel = unescapeXml(it) }

        val lensMatch = Regex("<exif:LensModel>(.*?)</exif:LensModel>").find(xmpXml)
        lensMatch?.groupValues?.getOrNull(1)?.let { exif.lensModel = unescapeXml(it) }
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun unescapeXml(str: String): String {
        return str.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
