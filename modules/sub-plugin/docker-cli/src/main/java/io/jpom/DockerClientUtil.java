/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import io.jpom.util.LogRecorder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class DockerClientUtil {


	public static void copyArchiveFromContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder, String resultFile, String resultFileOut) {
		logRecorder.info("download file from : {}", resultFile);
		try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(containerId, resultFile).exec();
			 TarArchiveInputStream tarStream = new TarArchiveInputStream(stream)) {
			TarArchiveEntry tarArchiveEntry;
			while ((tarArchiveEntry = tarStream.getNextTarEntry()) != null) {
				if (!tarStream.canReadEntryData(tarArchiveEntry)) {
					logRecorder.info("不能读取tarArchiveEntry");
				}
				if (tarArchiveEntry.isDirectory()) {
					continue;
				}
				String archiveEntryName = tarArchiveEntry.getName();
				// 截取第一级目录
				archiveEntryName = StrUtil.subAfter(archiveEntryName, StrUtil.SLASH, false);
				// 可能中包含文件 使用原名称
				archiveEntryName = StrUtil.emptyToDefault(archiveEntryName, tarArchiveEntry.getName());
				//logRecorder.info("tarArchiveEntry's name: {}", archiveEntryName);
				File currentFile = FileUtil.file(resultFileOut, archiveEntryName);
				FileUtil.mkParentDirs(currentFile);
				IoUtil.copy(tarStream, new FileOutputStream(currentFile));
			}
		} catch (Exception e) {
			logRecorder.error("无法获取容器执行结果文件: {}", e);
		}
	}

	/**
	 * 删除容器
	 *
	 * @param dockerClient docker 连接
	 * @param containerId  容器ID
	 */
	public static void removeContainerCmd(DockerClient dockerClient, String containerId) {
		if (containerId == null) {
			return;
		}
		// 清除容器
		dockerClient.removeContainerCmd(containerId)
				.withRemoveVolumes(true)
				.withForce(true)
				.exec();
	}
}
