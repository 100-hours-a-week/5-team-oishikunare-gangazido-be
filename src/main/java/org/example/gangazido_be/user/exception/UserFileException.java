package org.example.gangazido_be.user.exception;

public class UserFileException extends UserException {
	public static final String FILE_TOO_LARGE = "profile_image_too_large";
	public static final String INVALID_FILE_TYPE = "invalid_file_type";
	public static final String FILE_UPLOAD_ERROR = "file_upload_error";

	public UserFileException(String errorCode, String message) {
		super(errorCode, message);
	}

	public static UserFileException fileTooLarge() {
		return new UserFileException(FILE_TOO_LARGE, "profile_image_too_large");
	}

	public static UserFileException invalidFileType() {
		return new UserFileException(INVALID_FILE_TYPE, "invalid_file_type");
	}

	public static UserFileException uploadError(String details) {
		return new UserFileException(FILE_UPLOAD_ERROR, "file_upload_error");
	}
}
