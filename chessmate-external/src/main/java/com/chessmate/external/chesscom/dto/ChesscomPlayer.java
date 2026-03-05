package com.chessmate.external.chesscom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChesscomPlayer {

    @JsonProperty("player_id")
    private Long playerId;

    @JsonProperty("@id")
    private String id;

    @JsonProperty("url")
    private String url;

    @JsonProperty("username")
    private String username;

    @JsonProperty("title")
    private String title;

    @JsonProperty("status")
    private String status;

    @JsonProperty("name")
    private String name;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("location")
    private String location;

    @JsonProperty("country")
    private String country;

    @JsonProperty("joined")
    private Long joined;

    @JsonProperty("last_online")
    private Long lastOnline;

    @JsonProperty("followers")
    private Integer followers;

    @JsonProperty("is_streamer")
    private Boolean isStreamer;

    @JsonProperty("twitch_url")
    private String twitchUrl;

    @JsonProperty("fide")
    private Integer fide;
}
