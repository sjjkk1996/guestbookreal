package org.zerock.guestbook.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import org.zerock.guestbook.dto.GuestbookDTO;
import org.zerock.guestbook.dto.PageRequestDTO;
import org.zerock.guestbook.dto.PageResultDTO;
import org.zerock.guestbook.entity.Guestbook;
import org.zerock.guestbook.entity.QGuestbook;
import org.zerock.guestbook.repository.GuestbookRepository;

import java.util.Optional;
import java.util.function.Function;

@Service
@Log4j2
@RequiredArgsConstructor
public class GuestbookServiceImpl implements GuestbookService{

    private final GuestbookRepository guestbookRepository;
    //의존성 주입이 필요한 필드를 final로 선언하여 불변하게 사용 순환참조를 막기위함
    //생성자를 통하여 스프링빈을 생성할 때 순환참조가 발생. 그래서 final을 붙임
    //setter 즉, @Autowired를 사용할 경우, setter를 부르는 생성자보다 불분명
    @Override
    public Long register(GuestbookDTO dto) {
        log.info("DTO...................");
        log.info(dto);
        Guestbook entity = dtoToEntity(dto);
        log.info(entity);
        guestbookRepository.save(entity);
        //repoisitory interface findbyid save 이런게 들어가있음
        return entity.getGno();
    }

    @Override
    public PageResultDTO<GuestbookDTO, Guestbook> getList(PageRequestDTO dto) {
        //화면에 페이지 처리와 필요한 값들을 생성
        Pageable pageable = dto.getPageable(Sort.by("gno").descending());

        BooleanBuilder booleanBuilder = getSearch(PageRequestDTO.builder().build());
//        이줄 맞는지 모르겠음 ********************************************************************

        //JPA처리 결과인 Page<Entity>객체 생성
        Page<Guestbook> result = guestbookRepository.findAll(booleanBuilder, pageable);

        //JPA로부터 처리된 결과에 Entity를 DTO로 변형하는 처리부분
        Function<Guestbook, GuestbookDTO> fn = (entity -> entityToDto(entity));

        //위에서 만든 2가지를 Page
        return new PageResultDTO<>(result, fn);
        //entity 서비스 jpa 사이에서 쓰임
        //dto 서비스 컨트롤러 사이에서 쓰임

        
    }
    //<>제네릭을 쓰는 이유
    @Override
    public GuestbookDTO read(Long gno){
        Optional<Guestbook> result = guestbookRepository.findById(gno);
        return result.isPresent()?entityToDto(result.get()):null;
    }

    @Override
    public void remove(Long gno) {

        guestbookRepository.deleteById(gno);
    }
    @Override
    public void modify(GuestbookDTO dto) {
        Optional<Guestbook> result = guestbookRepository.findById(dto.getGno());
        if (result.isPresent()) {
            Guestbook entity = result.get();
            entity.changeTitle(dto.getTitle());
            entity.changeContent(dto.getContent());
            guestbookRepository.save(entity);
        }
    }
    private BooleanBuilder getSearch(PageRequestDTO requestDTO) {
        String type = requestDTO.getType();
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        QGuestbook qGuestbook = QGuestbook.guestbook;
        String keyWord = requestDTO.getKeyword();
        BooleanExpression expression = qGuestbook.gno.gt(0L);
        booleanBuilder.and(expression);
        if (type == null || type.trim().length() == 0) {
            return booleanBuilder;
        }
        BooleanBuilder conditionBuilder = new BooleanBuilder();
        if (type.contains("t")){
            conditionBuilder.or(qGuestbook.title.contains(keyWord));
        }
        if (type.contains("c")) {
            conditionBuilder.or(qGuestbook.content.contains(keyWord));
        }
        if (type.contains("w")) {
            conditionBuilder.or(qGuestbook.writer.contains(keyWord));
        }
        booleanBuilder.and(conditionBuilder);
        return booleanBuilder;
        }

    }

