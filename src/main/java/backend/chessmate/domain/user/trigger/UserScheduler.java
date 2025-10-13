package backend.chessmate.domain.user.trigger;

import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    @Value("${spring.data.redis.key.perf_key_base}")
    private String REDIS_PERF_KEY_BASE;

    @Value("${spring.data.redis.key.games_key_base}")
    private String REDIS_GAMES_KEY_BASE;

    @Value("${lichess.ttl.perf}")
    private long perfTTL;

    @Value("${lichess.ttl.games}")
    private long gamesTTL;

    private final UserRepository userRepository;
//    private final StreakService streakService;
//
//
//
//    @Scheduled(cron = "0 0/30 * * * ?")  //30분마다
//    public void updateTodayCache() {
//        List<User> users = userRepository.findAll();
//        for (User u : users) {
//            streakService.updateStreaks(u);
//        }
//    }




//    //1시간을 간격으로 사용자의 각 게임 타입별 퍼포먼스를 레디스에 업데이트
//    @Scheduled(cron = "0 0 * * * *")
//    public void updateUserPerf() {
//
//        List<User> users = userRepository.findAll();
//        for (User user : users) {
//            cacheService.userPerfCache(user);
//        }
//    }
//
//    //1시간을 간격으로 사용자의 한달 치 게임 정보를 레디스에 업데이트 (사용자 선호 첫 수 - 사용 횟수, 사용자 선호 오프닝 - 사용 횟수)
//    @Scheduled(cron = "0 0 * * * *")
//    public void updateUserGamesInfo() {
//        List<User> users = userRepository.findAll();
//        for (User user : users) {
//            cacheService.userGamesInfo(user);
//        }
//    }



}
