package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.appConfig.AppConfig;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {
        if (member.hasConnectedInstaMember() == false) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if (member.getInstaMember().getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }


        List<LikeablePerson> fromLikeablePeopleList = member.getInstaMember().getFromLikeablePeople();
        if (fromLikeablePeopleList.size() >= 10) {
            return RsData.of("F-3", "호감상대는 최대 10명까지 등록 가능합니다.");
        }

        InstaMember fromInstaMember = member.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        Optional<LikeablePerson> existingLikeablePerson = fromInstaMember.getFromLikeablePeople().stream()
                .filter(lp -> lp.getToInstaMember().getUsername().equals(username))
                .findFirst();

        LikeablePerson oldLikeablePerson = likeablePersonRepository.findByFromInstaMemberIdAndToInstaMember_username(member.getInstaMember().getId(), username);
        if (oldLikeablePerson != null) {
            oldLikeablePerson.setAttractiveTypeCode(attractiveTypeCode);
            likeablePersonRepository.save(oldLikeablePerson);

            return RsData.of("S-2", "%s에 대한 호감표시가 수정되었습니다.".formatted(username), oldLikeablePerson);
        }


        LikeablePerson likeablePerson;
        if (existingLikeablePerson.isPresent()) {
            // Update the existing likeable person with the new attractiveTypeCode value
            likeablePerson = existingLikeablePerson.get();
            likeablePerson.setAttractiveTypeCode(attractiveTypeCode);
        } else {
            likeablePerson = LikeablePerson
                    .builder()
                    .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                    .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                    .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                    .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                    .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                    .build();
            fromInstaMember.addFromLikeablePerson(likeablePerson);
            toInstaMember.addToLikeablePerson(likeablePerson);
        }

        likeablePersonRepository.save(likeablePerson); // 저장

        if (!fromLikeablePeopleList.contains(likeablePerson)) {
            return RsData.of("F-4", "Failed to add the likeable person to the user's list of likeable people.");
        }

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    public List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId) {
        return likeablePersonRepository.findByFromInstaMemberId(fromInstaMemberId);
    }

    public Optional<LikeablePerson> findById(Long id) {
        return likeablePersonRepository.findById(id);
    }

    @Transactional
    public RsData cancel(LikeablePerson likeablePerson) {
        // 사용자가 생성한 '좋아요' 삭제
        likeablePerson.getFromInstaMember().removeFromLikeablePerson(likeablePerson);

        // 사용자가 받은 '좋아요' 삭제
        likeablePerson.getToInstaMember().removeToLikeablePerson(likeablePerson);
        likeablePersonRepository.delete(likeablePerson);

        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }

    //@Transactional
    public RsData canCancel(Member actor, LikeablePerson likeablePerson) {
        if (likeablePerson == null) {
            return RsData.of("F-1", "이미 삭제되었습니다.");
        }

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();
        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId) {
            return RsData.of("F-2", "권한이 없습니다.");
        }

        return RsData.of("S-1", "삭제가능합니다.");
    }

    public RsData canLike(Member actor, String username, int attractiveTypeCode) {
        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (fromInstaMember.getUsername().equals(username)) {
            return RsData.of("F-2", "본인을 호감상대로 등록할 수 없습니다.");
        }

        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = fromInstaMember.getFromLikeablePeople();

        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson != null && fromLikeablePerson.getAttractiveTypeCode() == attractiveTypeCode) {
            return RsData.of("F-3", "이미 %s님에 대해서 호감표시를 했습니다.".formatted(username));
        }

        long likeablePersonFromMax = AppConfig.getLikeablePersonFromMax();

        if (fromLikeablePeople.size() >= likeablePersonFromMax) {
            return RsData.of("F-4", "최대 %d명에 대해서만 호감표시가 가능합니다.".formatted(likeablePersonFromMax));
        }

        if (fromLikeablePerson != null) {
            return RsData.of("S-2", "%s님에 대해서 호감표시가 가능합니다.".formatted(username));
        }

        return RsData.of("S-1", "%s님에 대해서 호감표시가 가능합니다.".formatted(username));
    }

    @Transactional
    public RsData modifyAttractive(Member member, String username, int attractiveTypeCode) {
        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = member.getInstaMember().getFromLikeablePeople();

        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson == null) {
            return RsData.of("F-7", "호감표시를 하지 않았습니다.");
        }

        String oldAttractiveTypeDisplayName = fromLikeablePerson.getAttractiveTypeDisplayName();

        fromLikeablePerson.setAttractiveTypeCode(attractiveTypeCode);
        likeablePersonRepository.save(fromLikeablePerson);

        String newAttractiveTypeDisplayName = fromLikeablePerson.getAttractiveTypeDisplayName();

        return RsData.of("S-3", "%s님에 대한 호감사유를 %s에서 %s(으)로 변경합니다.".formatted(username, oldAttractiveTypeDisplayName, newAttractiveTypeDisplayName));
    }

    public Optional<LikeablePerson> findByFromInstaMember_usernameAndToInstaMember_username(String fromInstaMemberUsername, String toInstaMemberUsername) {
        return likeablePersonRepository.findByFromInstaMember_usernameAndToInstaMember_username(fromInstaMemberUsername, toInstaMemberUsername);
    }

}
