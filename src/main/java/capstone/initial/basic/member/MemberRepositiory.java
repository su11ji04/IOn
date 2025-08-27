package capstone.initial.basic.member;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberRepositiory {
    private static Map<Long, Member> store = new HashMap<>();
    private static Long sequence = 0L;

    private static final MemberRepositiory instance = new MemberRepositiory();

    public static MemberRepositiory getInstance() {
        return instance;
    }

    private MemberRepositiory() {}

    public Member save(Member member) {
        member.setId(++sequence);
        store.put(member.getId(), member);
        return member;
    }

    public Member findById(Long id){
        return store.get(id);
    }

    public List<Member> findAll(){
        return  new ArrayList<>(store.values());
    }

    public void clearStore(){
        store.clear();
    }

}
