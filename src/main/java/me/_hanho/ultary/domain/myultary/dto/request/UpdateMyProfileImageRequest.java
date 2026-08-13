package me._hanho.ultary.domain.myultary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyProfileImageRequest {

	/** 사전 업로드한 fileId */
	private Long profileFileId;

	/** true면 profile_file_id NULL */
	private Boolean removeProfileFile;
}
