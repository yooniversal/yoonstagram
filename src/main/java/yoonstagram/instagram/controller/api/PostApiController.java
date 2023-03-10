package yoonstagram.instagram.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yoonstagram.instagram.config.auth.PrincipalDetails;
import yoonstagram.instagram.domain.*;
import yoonstagram.instagram.domain.dto.*;
import yoonstagram.instagram.service.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostApiController {

    private final UserService userService;
    private final PostService postService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final NotificationService notificationService;
    private final FollowService followService;

    @GetMapping("/post/{postId}")
    public ResponseEntity<?> postInfo (@PathVariable("postId") Long postId,
                                       @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return new ResponseEntity<>(postService.getPostInfoDto(postId, principalDetails.getUser().getId()), HttpStatus.OK);
    }

    @PostMapping("/post/{postId}/likes")
    public ResponseEntity<?> likes(@PathVariable("postId") Long postId,
                                   @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User currentUser = principalDetails.getUser();
        likeService.likes(postId, currentUser.getId());

        Post post = postService.findOneById(postId);
        if(!post.getUser().getId().equals(currentUser.getId()))
            notificationService.save(post.getUser(), currentUser, post.getImageUrl(), NotificationStatus.LIKE, postId);

        return new ResponseEntity<>("좋아요 성공", HttpStatus.OK);
    }

    @DeleteMapping("/post/{postId}/likes")
    public ResponseEntity<?> unLikes(@PathVariable("postId") Long postId,
                                     @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User currentUser = principalDetails.getUser();
        likeService.unLikes(postId, currentUser.getId());

        Post post = postService.findOneById(postId);
        if(!post.getUser().getId().equals(currentUser.getId()))
            notificationService.cancel(currentUser.getId(), NotificationStatus.LIKE, postId);

        return new ResponseEntity<>("좋아요 취소 성공", HttpStatus.OK);
    }

    @GetMapping("/post")
    public ResponseEntity<?> mainStory(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<Post> posts = postService.getPostsOfUser(principalDetails.getUser().getId());
        List<PostInfoDto> postInfoDtos = new ArrayList<>();
        for(Post post : posts)
            postInfoDtos.add(postService.getPostInfoDto(post.getId(), principalDetails.getUser().getId()));

        return new ResponseEntity<>(postInfoDtos, HttpStatus.OK);
    }

    @GetMapping("/post/tag")
    public ResponseEntity<?> searchTag(@RequestParam String tag,
                                       @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<Post> postsWithTag = postService.getPostsWithTag(tag);
        List<PostInfoDto> postDtoList = new ArrayList<>();
        for(Post post : postsWithTag) {
            postDtoList.add(postService.getPostInfoDto(post.getId(), principalDetails.getUser().getId()));
        }
        return new ResponseEntity<>(postDtoList, HttpStatus.OK);
    }

    @GetMapping("/post/likes")
    public ResponseEntity<?> getLikesPostOfUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        User currentUser = principalDetails.getUser();
        List<Like> likes = likeService.findLikesWithUser(currentUser.getId());
        List<LikeDto> likeDtos = new ArrayList<>();
        for(Like like : likes) {
            LikeDto likeDto = new LikeDto(like);
            likeDtos.add(likeDto);
        }
        return new ResponseEntity<>(likeDtos, HttpStatus.OK);
    }

    @GetMapping("/post/likes/{postId}")
    public ResponseEntity<?> getLikesPost(@PathVariable("postId") Long postId,
                                          @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long currentUserId = principalDetails.getUser().getId();
        List<Like> likes = likeService.findLikesWithPostId(postId);
        List<LikeDto> likeDtos = new ArrayList<>();
        for(Like like : likes) {
            Long likeUserId = like.getUser().getId();
            List<User> followings = followService.getFollowings(currentUserId);
            boolean follow = followings.contains(userService.findOneById(likeUserId));
            LikeDto likeDto = new LikeDto(like, currentUserId.equals(likeUserId), follow);
            likeDtos.add(likeDto);
        }
        return new ResponseEntity<>(likeDtos, HttpStatus.OK);
    }

    @GetMapping("/post/popular")
    public ResponseEntity<?> getPopularPost() {
        List<Post> posts = postService.getAllPosts();
        Collections.sort(posts);

        List<LikeDto> likeDtos = new ArrayList<>();
        for (Post post : posts) {
            LikeDto likeDto = new LikeDto(post);
            likeDtos.add(likeDto);
        }

        return new ResponseEntity<>(likeDtos, HttpStatus.OK);
    }

    @GetMapping("/upload")
    public ResponseEntity<?> uploadInfo (
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        return new ResponseEntity<>(postService.getBlankPostInfoDto(principalDetails.getUser()), HttpStatus.OK);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPost(@RequestParam("uploadText") String text,
                                        @RequestParam("uploadTag") String tag,
                                        @RequestParam("uploadImgUrl") MultipartFile file,
                                        @AuthenticationPrincipal PrincipalDetails principalDetails) {

        User user = principalDetails.getUser();

        UploadPostDto uploadPostDto = new UploadPostDto();
        uploadPostDto.setText(text);
        uploadPostDto.setTag(tag);

        postService.upload(uploadPostDto, file, user.getId());

        return new ResponseEntity<>("업로드 성공", HttpStatus.OK);
    }

    @PostMapping("/edit")
    public ResponseEntity<?> editPost(@ModelAttribute PostUpdateDto postUpdateDto) {
        postService.update(postUpdateDto);
        return new ResponseEntity<>("업데이트 성공", HttpStatus.OK);
    }

    @DeleteMapping("/post/delete/{postId}")
    public ResponseEntity<?> postDelete(@PathVariable("postId") Long postId) {

        Post post = postService.findOneById(postId);
        List<Comment> comments = post.getComment();

        // 댓글 삭제
        for(Comment comment : comments) {
            commentService.deleteComment(comment.getId());
        }

        // 좋아요 삭제
        List<Like> likes = post.getLikes();
        for (Like like : likes) {
            likeService.unLikes(like.getPost().getId(), like.getUser().getId());
        }

        // 알림 삭제
        notificationService.cancel(post.getUser().getId(), NotificationStatus.LIKE, postId);

        // 게시물 삭제
        postService.delete(postId);

        return new ResponseEntity<>("게시물 삭제 성공", HttpStatus.OK);
    }

}
