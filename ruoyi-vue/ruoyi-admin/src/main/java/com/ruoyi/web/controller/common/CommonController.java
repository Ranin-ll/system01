package com.ruoyi.web.controller.common;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;

/**
 * 通用请求处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

    private static final String FILE_DELIMETER = ",";

    /**
     * 通用下载请求
     * 
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete)
            {
                FileUtils.deleteFile(filePath);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files)
            {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMETER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMETER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMETER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMETER));
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            // 自行从原始查询串按 UTF-8 解析 resource：本环境 Tomcat 对查询串的中文会被按 ASCII/ISO 误解码成 '?'（不可逆），
            // 而 getQueryString() 仍是客户端发出的百分号编码原文，这里用 UTF-8 重新解码即可拿到正确中文路径。
            String rawResource = extractResourceFromQuery(request);
            if (rawResource != null) {
                resource = rawResource;
            }
            // 课程附件（安装包 / 压缩包 / 镜像）不在默认下载白名单里，
            // 这里额外放行课程附件扩展名 —— 否则「课程里传得上去、实习生下载下来却是空文件」。
            if (!FileUtils.checkAllowDownload(resource, MimeTypeUtils.COURSE_ASSET_EXTENSION))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 课程资料自 2026-09-24 起支持「绝对路径」存储（可指向任意磁盘，便于后续迁移到其它盘）。
            // 绝对路径直接规范化并校验落盘范围；非绝对路径仍按 RuoYi 约定走 /profile 相对解析。
            String downloadPath;
            String downloadName;
            if (new File(resource).isAbsolute())
            {
                Path abs = Paths.get(resource).toAbsolutePath().normalize();
                Path profileRoot = Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
                Path courseRoot = Paths.get(RuoYiConfig.getCourseRoot()).toAbsolutePath().normalize();
                if (!abs.startsWith(profileRoot) && !abs.startsWith(courseRoot))
                {
                    throw new Exception(StringUtils.format("资源文件({})不在允许的根目录内，不允许下载。 ", resource));
                }
                downloadPath = abs.toString();
                downloadName = abs.getFileName().toString();
            }
            else
            {
                // 本地资源路径
                String localPath = RuoYiConfig.getProfile();
                // 数据库资源地址
                downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
                // 下载名称
                downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            }
            // ⚠️ 先把「文件到底在不在」校验掉，再写任何响应头。
            // 否则 FileUtils.writeBytes 的 finally 会 close 输出流，等于提前把响应提交掉：
            // 之后就算抛异常，isCommitted() 也已为 true，再也回不了错误体，
            // 前端只会收到「200 + 空 body」→ 保存出一个 0 字节的空文件。
            File target = new File(downloadPath);
            if (!target.exists() || !target.isFile())
            {
                throw new Exception("文件不存在或已被删除：" + downloadName);
            }
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
            // ⚠️ 这里以前只打日志、不写响应：前端拿到的是「HTTP 200 + 空 body」，
            // blobValidate 判定为文件 → 用户保存出一个 0 字节、文件名 undefined 的空文件，
            // 且看不到任何错误提示。现在按 RuoYi 约定回 JSON 错误体（HTTP 200 + code 500），
            // 前端 download.js 的 printErrMsg 就能把真实原因弹出来。
            if (!response.isCommitted())
            {
                response.reset();
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("utf-8");
                String message = StringUtils.isNotEmpty(e.getMessage()) ? e.getMessage() : "未知错误";
                message = message.replace("\\", "\\\\").replace("\"", "\\\"");
                // ⚠️ 必须用输出流：进入这里之前已经调用过 response.getOutputStream()
                // （writeBytes 的实参），此时再调 getWriter() 会抛
                // IllegalStateException: getWriter() has already been called for this response，
                // 结果又变成「200 + 空 body」，白改一场。
                response.getOutputStream().write(("{\"code\":500,\"msg\":\"文件下载失败：" + message + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 从原始查询串中按 UTF-8 取出 resource 参数值（绕过 Tomcat 对查询串的中文解码）。
     * <p>本环境 Tomcat 会把查询串里的中文按 ASCII/ISO 误解码成 '?'（不可逆），导致中文路径找不到文件。
     * {@code HttpServletRequest.getQueryString()} 返回的是客户端发出的百分号编码原文，
     * 这里用 UTF-8 重新解码即可得到正确的中文路径。</p>
     */
    private String extractResourceFromQuery(HttpServletRequest request)
    {
        String qs = request.getQueryString();
        if (StringUtils.isEmpty(qs))
        {
            return null;
        }
        for (String pair : qs.split("&"))
        {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if ("resource".equals(key))
            {
                String val = eq >= 0 ? pair.substring(eq + 1) : "";
                try
                {
                    return java.net.URLDecoder.decode(val, "UTF-8");
                }
                catch (Exception ignored)
                {
                    return val;
                }
            }
        }
        return null;
    }
}
