package com.ruoyi.common.utils.file;

/**
 * 媒体类型工具类
 * 
 * @author ruoyi
 */
public class MimeTypeUtils
{
    public static final String IMAGE_PNG = "image/png";

    public static final String IMAGE_JPG = "image/jpg";

    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String IMAGE_BMP = "image/bmp";

    public static final String IMAGE_GIF = "image/gif";
    
    public static final String[] IMAGE_EXTENSION = { "bmp", "gif", "jpg", "jpeg", "png" };

    public static final String[] FLASH_EXTENSION = { "swf", "flv" };

    public static final String[] MEDIA_EXTENSION = { "swf", "flv", "mp3", "wav", "wma", "wmv", "mid", "avi", "mpg",
            "asf", "rm", "rmvb" };

    public static final String[] VIDEO_EXTENSION = { "mp4", "avi", "rmvb" };

    public static final String[] DEFAULT_ALLOWED_EXTENSION = {
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png",
            // word excel powerpoint
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt",
            // 压缩文件
            "rar", "zip", "gz", "bz2",
            // 视频格式
            "mp4", "avi", "rmvb",
            // pdf
            "pdf" };

    /**
     * 课程资料「文档 / 附件」类允许的扩展名（2026-09-23 扩充）。
     *
     * <p>课程要能给实习生下发交付工具安装包、环境压缩包，所以除了文档格式，
     * 还放开了压缩包、安装包与镜像。</p>
     *
     * <p>⚠️ 这份清单是**上传 + 下载的共用口径**：
     * {@code CourseContentServiceImpl.DOCUMENT_EXTENSIONS} 直接引用它，
     * {@code CommonController.resourceDownload} 也把它作为 DEFAULT 白名单之外的额外放行项。
     * 两边必须一致 —— 否则会出现「传得上去、下载回来却是 0 字节、文件名变成 undefined」
     * （下载接口判非法后异常被吞，响应停在 200 + 空 body）。</p>
     */
    public static final String[] COURSE_ASSET_EXTENSION = {
            // 文档
            "pdf", "doc", "docx", "ppt", "pptx", "txt", "xls", "xlsx",
            // 压缩包
            "zip", "7z", "rar", "tar", "gz", "tgz",
            // 安装包
            "exe", "msi", "dmg", "pkg", "deb", "rpm",
            // 镜像与其它
            "iso", "apk", "jar", "war", "bin", "sh"
    };

    public static String getExtension(String prefix)
    {
        switch (prefix)
        {
            case IMAGE_PNG:
                return "png";
            case IMAGE_JPG:
                return "jpg";
            case IMAGE_JPEG:
                return "jpeg";
            case IMAGE_BMP:
                return "bmp";
            case IMAGE_GIF:
                return "gif";
            default:
                return "";
        }
    }
}
