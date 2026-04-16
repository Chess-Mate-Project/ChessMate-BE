package com.chessmate.api.user.dto;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SearchUsersResponse {

    private final List<UserSearchProfileResponse> users;


}
