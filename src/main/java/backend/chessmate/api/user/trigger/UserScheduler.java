//package backend.chessmate.api.user.trigger;
//
//import backend.chessmate.api.user.entity.User;
//import backend.chessmate.api.oauth.repository.UserRepository;
//import backend.chessmate.api.user.service.StatService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//
//import java.util.*;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class UserScheduler {
//
//    @Value("${spring.data.redis.key.perf_key_base}")
//    private String REDIS_PERF_KEY_BASE;
//
//    @Value("${spring.data.redis.key.games_key_base}")
//    private String REDIS_GAMES_KEY_BASE;
//
//    @Value("${lichess.ttl.perf}")
//    private long perfTTL;
//
//    @Value("${lichess.ttl.games}")
//    private long gamesTTL;
//
//    private final UserRepository userRepository;
//    private final StatService statService;
//
//
//
//    @Scheduled(cron = "0 0/30 * * * ?")  //30분마다
//    public void updateTodayCache() {
//        List<User> users = userRepository.findAll();
//        for (User u : users) {
//            statService.asyncGames(u);
//        }
//    }
//
//}
