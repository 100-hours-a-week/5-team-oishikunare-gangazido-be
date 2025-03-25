package org.example.gangazido_be.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class UserFileService {

	// 새로운 업로드 경로 설정
	private final String uploadDir = "uploads/user";

	/**
	 * 사용자 프로필 이미지 저장
	 *
	 * @param profileImage 업로드된 프로필 이미지 파일
	 * @return 저장된 이미지 경로
	 */
	public String saveProfileImage(MultipartFile profileImage) {
		if (profileImage == null || profileImage.isEmpty()) {
			return null;
		}

		try {
			// 업로드 디렉토리 생성
			Path uploadPath = Paths.get(uploadDir);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			// 고유한 파일명 생성
			String originalFilename = profileImage.getOriginalFilename();
			String extension = "";
			if (originalFilename != null && originalFilename.contains(".")) {
				extension = originalFilename.substring(originalFilename.lastIndexOf("."));
			}
			String filename = UUID.randomUUID() + extension;

			// 파일 저장
			Path filePath = uploadPath.resolve(filename);
			Files.copy(profileImage.getInputStream(), filePath);

			// URL 경로 반환
			return "/uploads/user/" + filename;
		} catch (IOException e) {
			throw new RuntimeException("프로필 이미지 저장 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	/**
	 * 이미지 파일 삭제
	 *
	 * @param imagePath 삭제할 이미지 경로
	 * @return 삭제 성공 여부
	 */
	public boolean deleteImage(String imagePath) {
		if (imagePath == null || imagePath.isEmpty()) {
			return false;
		}

		try {
			// URL에서 파일명 추출
			String filename = imagePath.substring(imagePath.lastIndexOf("/") + 1);
			Path filePath = Paths.get(uploadDir).resolve(filename);

			// 파일 존재 확인 후 삭제
			if (Files.exists(filePath)) {
				Files.delete(filePath);
				return true;
			}
			return false;
		} catch (IOException e) {
			throw new RuntimeException("이미지 삭제 중 오류가 발생했습니다: " + e.getMessage());
		}
	}
}
