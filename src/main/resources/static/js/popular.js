function storyLoad() {
    $.ajax({
        url: `/api/post/popular`,
        dataType: "json"
    }).done(res => {
        res.forEach((post) => {
            let postItem = getLikesItem(post);
            $("#popular-box").append(postItem);
        });
    }).fail(error => {
        console.log("오류", error);
    });
}

storyLoad();

function getLikesItem(post) {
    let item = `
        <div class="img-box" onclick="postPopup(${post.postId}, '.modal-post')" >                   
            <img src="/upload/${post.postImageUrl}" onerror="this.src='/img/default_profile.jpg';" />
                <div class="comment">
                    <a> <i class="fas fa-heart"></i><span>${post.likeCount}</span></a>
                </div>
        </div>
    `;

    return item;
}

//포스트
function postPopup(postId, obj) {
    $(obj).css("display", "flex");

    $.ajax({
        url: "/api/post/" + postId,
        dataType: "json"
    }).done(res => {
        let item = getPostModalInfo(res);
        $("#postInfoModal").append(item);
    }).fail(error => {
        console.log("post 정보 불러오기 오류", error);
    });
}

function modalClose() {
    $(".modal-post").css("display", "none");
    location.reload();
}

function getPostModalInfo(postInfoDto) {
    let diffentTime = function () {
        const currentTime = new Date();
        const postTimeStamp = new Date(postInfoDto.date);
        const postTimeInMillis = postTimeStamp.getTime();
        const postTime = new Date(postTimeInMillis);

        const timeDiff = currentTime - postTime;
        const msDiff = timeDiff;
        const secDiff = msDiff / 1000;
        const minDiff = secDiff / 60;
        const hourDiff = minDiff / 60;
        const dayDiff = hourDiff / 24;
        const monthDiff = dayDiff / 30;
        const yearDiff = monthDiff / 12;

        let timeAgo = "";
        if (yearDiff >= 1) {
            timeAgo = Math.floor(yearDiff) + "년 전";
        } else if (monthDiff >= 1) {
            timeAgo = Math.floor(monthDiff) + "개월 전";
        } else if (dayDiff >= 1) {
            timeAgo = Math.floor(dayDiff) + "일 전";
        } else if (hourDiff >= 1) {
            timeAgo = Math.floor(hourDiff) + "시간 전";
        } else if (minDiff >= 1) {
            timeAgo = Math.floor(minDiff) + "분 전";
        } else {
            timeAgo = "지금";
        }

        return timeAgo;
    }
    let principalId = $("#principalId").val();
    let item = `
    <div class="subscribe-header">
            <a href="/user/profile?id=${postInfoDto.postUploader.id}"><img class="post-img-profile pic" src="/profile_imgs/${postInfoDto.postUploader.profileImgUrl}" onerror="this.src='/img/default_profile.jpg'""></a>  
            <span>${postInfoDto.postUploader.name}</span> `;
    item += `<button class="exit" onclick="modalClose()"><i class="fas fa-times"></i></button>`
    if(postInfoDto.uploader) {
        item += `<button class="edit" onclick="postEditPopup('.post-edit-modal-info', ${postId})'"><i class="far fa-edit"></i></button>`
    }
    item += `
    </div>
    <div class="post-box">
	    <div class="subscribe__img">
		    <img src="/upload/${postInfoDto.postImgUrl}" />
	    </div>
	    <div class="post-div">
	    <div>
            <div class="post-info">
                    <div class="text post-text-area"> `;
            if(postInfoDto.likeState) {
                item += `<i class="fas fa-heart heart active" id="storyLikeIcon" onclick="toggleLike(${postInfoDto.id})"></i>
                         <span class="like-text" onclick="postLikeInfoModal(${postInfoDto.id})">좋아요 <span id="likeCount" style="font-size:inherit;">${postInfoDto.likeCount}</span>개</span>`;
            } else {
                item += `<i class="far fa-heart heart" id="storyLikeIcon" onclick="toggleLike(${postInfoDto.id})"></i>
                         <span class="like-text" onclick="postLikeInfoModal(${postInfoDto.id})">좋아요 <span id="likeCount" style="font-size:inherit;">${postInfoDto.likeCount}</span>개</span>`;
            }
            item += `
                    </div>
                    <div class="text post-text-area">
                        <span>${postInfoDto.text}</span>
                    </div>
                    <div class="tag post-text-area">`;
            let arr = postInfoDto.tag.split(',');

            for(let i = 0; i < arr.length; i++) {
                item += `<span class="tag-span" onclick="location.href='/post/search/${arr[i]}'">#${arr[i]} </span>`;
            }
            item += `
                    </div>
                </div>
                <div class="subscribe__img post-text-area">
                    <span class="post-time">${diffentTime()}</span>
                </div>
        </div>
        <div class="comment-section" >
                <ul class="comments" id="storyCommentList-${postInfoDto.id}">`;
    postInfoDto.comments.forEach((comment)=>{
        item += `<li id="storyCommentItem-${comment.id}">
                    <a href="/user/profile?id=${comment.userId}">
                       <img class="comment-pic" src="/profile_imgs/${comment.imageUrl}" onerror="src='/img/default_profile.jpg'">
                    </a>
                    <span id="comment-text">
                       <span class="comment-span point-span">${comment.name}</span>${comment.text}
                    </span>`;
                    if(principalId == comment.userId) {
                        item += `<button onclick="deleteComment(${comment.id})" class="delete-comment-btn">
                                    <i class="fas fa-times"></i>
                                </button>`;
                    }
        item += `</li>`});
    item += `
                </ul>
            </div>
            </div>
            <div class="comment_input">
                    <input id="storyCommentInput-${postInfoDto.id}" class="input-comment-post" type="text" placeholder="댓글 달기..." >
                    <button type="button" class="submit-comment" onClick="addComment(${postInfoDto.id})">게시</button>
            </div>
        </div>
    </div>`;
    return item;
}
function toggleLike(postId) {
    let likeIcon = $("#storyLikeIcon");

    if (likeIcon.hasClass("far")) { // 좋아요
        $.ajax({
            type: "post",
            url: `/api/post/${postId}/likes`,
            dataType: "text"
        }).done(res=>{
            let likeCountStr = $("#likeCount").html();
            let likeCount = Number(likeCountStr) + 1;
            $("#likeCount").html(likeCount);

            likeIcon.addClass("fas");
            likeIcon.addClass("active");
            likeIcon.removeClass("far");
        }).fail(error=>{
            console.log("오류", error);
        });
    } else { // 좋아요 취소
        $.ajax({
            type: "delete",
            url: `/api/post/${postId}/likes`,
            dataType: "text"
        }).done(res=>{
            let likeCountStr = $("#likeCount").html();
            let likeCount = Number(likeCountStr) - 1;
            $("#likeCount").html(likeCount);

            likeIcon.removeClass("fas");
            likeIcon.removeClass("active");
            likeIcon.addClass("far");
        }).fail(error=>{
            console.log("오류", error);
            alert(error.responseText);
        });
    }
}

//댓글 추가
function addComment(postId) {
    let commentInput = $(`#storyCommentInput-${postId}`);
    let commentList = $(`#storyCommentList-${postId}`);

    let data = {
        postId: postId,
        text: commentInput.val()
    }

    if (data.text === "") {
        alert("댓글을 작성해주세요!");
        return;
    }

    $.ajax({
        type: "post",
        url: "/api/comment",
        data: JSON.stringify(data),
        contentType: "application/json; charset=utf-8",
        dataType: "json"
    }).done(res=>{
        console.log("성공", res);
        let comment = res;
        let content = `
		    <li id="storyCommentItem-${comment.id}">
                 <a href="/user/profile?id=${comment.userId}">
                    <img class="comment-pic" src="/profile_imgs/${comment.imageUrl}" onerror="src='/img/default_profile.jpg'">
                 </a>
                 <span id="comment-text">
                    <span class="comment-span point-span">${comment.name}</span>${comment.text}
                 </span>`;
    content += `<button onclick="deleteComment(${comment.id})" class="delete-comment-btn">
                            <i class="fas fa-times"></i>
                </button>
            </li>`;
        commentList.append(content);
    }).fail(error=>{
        console.log("오류", error);
    });

    commentInput.val(""); // 인풋 필드를 깨끗하게 비워준다.
}

function deleteComment(commentId) {
    $.ajax({
        type: "delete",
        url: `/api/comment/${commentId}`
    }).done(res=>{
        console.log("성공", res);
        $(`#storyCommentItem-${commentId}`).remove();
    }).fail(error=>{
        console.log("오류", error);
    });
}