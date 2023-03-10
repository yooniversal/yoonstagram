package yoonstagram.instagram.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import yoonstagram.instagram.config.auth.PrincipalDetails;
import yoonstagram.instagram.domain.NotificationStatus;
import yoonstagram.instagram.domain.User;
import yoonstagram.instagram.domain.dto.SimpleUserDto;
import yoonstagram.instagram.service.FollowService;
import yoonstagram.instagram.service.NotificationService;
import yoonstagram.instagram.service.UserService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class FollowApiController {

    private final FollowService followService;
    private final UserService userService;
    private final NotificationService notificationService;

    @PostMapping("/follow/{userId}")
    public ResponseEntity<?> followUser(@PathVariable("userId") Long userId,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User currentUser = principalDetails.getUser();
        User fromUser = userService.findOneById(userId);
        followService.follow(currentUser.getId(), userId);
        notificationService.save(fromUser, currentUser, null, NotificationStatus.FOLLOW, 0L);
        return new ResponseEntity<>("팔로우 성공", HttpStatus.OK);
    }

    @DeleteMapping("/follow/{userId}")
    public ResponseEntity<?> unFollowUser(@PathVariable("userId") Long userId,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User currentUser = principalDetails.getUser();
        followService.unFollow(currentUser.getId(), userId);
        notificationService.cancel(currentUser.getId(), NotificationStatus.FOLLOW, userId);
        return new ResponseEntity<>("팔로우 취소 성공", HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/following")
    public ResponseEntity<?> followingOfUser(@PathVariable("userId") Long userId,
                                             @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<User> followings = followService.getFollowings(userId);
        List<SimpleUserDto> followingDtos = new ArrayList<>();
        for(User user : followings)
            followingDtos.add(new SimpleUserDto(user,
                    true,
                    user.getId().equals(principalDetails.getUser().getId())));
        return new ResponseEntity<>(followingDtos, HttpStatus.OK);
    }

    @GetMapping("/user/{userId}/follower")
    public ResponseEntity<?> followerOfUser(@PathVariable("userId") Long userId,
                                             @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<User> followers = followService.getFollowers(userId);
        List<User> followings = followService.getFollowings(userId);
        List<SimpleUserDto> followerDtos = new ArrayList<>();
        for(User user : followers)
            followerDtos.add(new SimpleUserDto(user,
                    followings.contains(userService.findOneById(user.getId())),
                    user.getId().equals(principalDetails.getUser().getId())));
        return new ResponseEntity<>(followerDtos, HttpStatus.OK);
    }
}