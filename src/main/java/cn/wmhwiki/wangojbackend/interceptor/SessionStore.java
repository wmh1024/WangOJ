package cn.wmhwiki.wangojbackend.interceptor;

import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import cn.wmhwiki.wangojbackend.constant.RedisTimeoutConstant;
import cn.wmhwiki.wangojbackend.util.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 閻ц缍嶆导姘崇樈鐎涙ê鍋嶉敍姘辨樊閹?{@code token -> userId} 娑?{@code userId -> tokens} 閸欏苯鎮滅槐銏犵穿閵?
 *
 * <p>閸欏苯鎮滅槐銏犵穿閻ㄥ嫭鍓版稊澶涚窗閺€鐟扮槕閵嗕線妾烽弶鍐︹偓浣稿灩閸欓攱妞傞崣顖欎簰閹稿鏁ら幋铚傜濞嗏剝鈧冩倫闁库偓閸忚泛鍙忛柈?Token閿?
 * 闁灝鍘?閺冄傜窗鐠囨繄鎴风紒顓熷瘮閺堝妫煬顐″敜"閵?/p>
 */
@Slf4j
@Component
public class SessionStore {

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisTimeoutConstant timeout;

    public SessionStore(StringRedisTemplate stringRedisTemplate, RedisTimeoutConstant timeout) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.timeout = timeout;
    }

    /**
     * 閺傛澘缂撴导姘崇樈楠炴儼绻戦崶?{@code Bearer <uuid>} 瑜般垹绱￠惃?Token閵?
     *
     * <p>Redis 娑擃厺绔村瀣╀簰 <b>瑜版帊绔撮崠?/b>閿涘牆骞撻幒澶庣箾鐎涙顑侀妴浣哥毈閸愭瑱绱氶惃?32 娴ｅ秴宕勯崗顓＄箻閸掕泛鐡?key閿?
     * 鐠囬攱鐪版笟?{@code TokenUtils.resolveToken} 娴兼艾缍婃稉鈧崠鏍ф倵閸愬秵鐓￠敍?
     * 閼汇儴绻栭柌宀€娲块幒銉ョ摠 {@code UUID.toString()} 閻ㄥ嫬鐢潻鐐茬摟缁楋箑鑸伴幀渚婄礉娑撱倛绔熷姝岀箼鐎甸€涚瑝娑撳绱?
     * 鐞涖劎骞囨稉?閼崇晫娅ヨぐ鏇氱稻娑撯偓鐠佸潡妫堕崣妞剧箽閹躲倖甯撮崣锝呮皑 401"閵?/p>
     */
    public String issue(Long userId) {
        String token = TokenUtils.normalize(UUID.randomUUID().toString());
        String tokenKey = RedisKeyConstant.TOKEN + token;
        stringRedisTemplate.opsForValue().set(tokenKey, String.valueOf(userId),
                timeout.getToken(), TimeUnit.MINUTES);

        String userTokensKey = RedisKeyConstant.USER_TOKENS + userId;
        stringRedisTemplate.opsForSet().add(userTokensKey, token);
        // 闂嗗棗鎮庨惃?TTL 閻ｃ儱銇囨禍?Token閿涘苯鑻熼崷銊嚞濮瑰倿鎽肩捄顖欒厬閹镐胶鐢荤紒顓熸埂閿涘牐顫?touch閿?
        stringRedisTemplate.expire(userTokensKey, timeout.getToken() * 2, TimeUnit.MINUTES);
        return "Bearer " + token;
    }

    /**
     * 濮ｅ繑顐肩拠閿嬬湴閸掗攱鏌婃导姘崇樈閺堝鏅ラ張鐔粹偓?
     *
     * <p>韫囧懘銆忛崥灞炬缂侇厽婀?{@code token -> userId} 娑?{@code userId -> tokens}閿?
     * 閸欘亞鐢婚張鐔峰閼板懍绱扮拋鈺冩暏閹?Token 闂嗗棗鎮庨崗鍫ｇ箖閺堢噦绱濈€佃壈鍤ч弨鐟扮槕/闂勫秵娼堥弮鑸垫￥濞夋洘澹掗柌蹇撴倫闁库偓閺冄傜窗鐠囨縿鈧?/p>
     */
    public void touch(String token, Long userId) {
        if (token == null || userId == null) {
            return;
        }
        stringRedisTemplate.expire(RedisKeyConstant.TOKEN + token,
                timeout.getToken(), TimeUnit.MINUTES);
        String userTokensKey = RedisKeyConstant.USER_TOKENS + userId;
        stringRedisTemplate.opsForSet().add(userTokensKey, token);
        stringRedisTemplate.expire(userTokensKey, timeout.getToken() * 2, TimeUnit.MINUTES);
    }

    /**
     * 閸氬﹪鏀㈤崡鏇氶嚋 Token閿涘牏娅ラ崙鐚寸礆閵?
     */
    public void revoke(String token) {
        if (token == null) {
            return;
        }
        String tokenKey = RedisKeyConstant.TOKEN + token;
        String userId = stringRedisTemplate.opsForValue().get(tokenKey);
        stringRedisTemplate.delete(tokenKey);
        if (userId != null) {
            stringRedisTemplate.opsForSet().remove(RedisKeyConstant.USER_TOKENS + userId, token);
        }
    }

    /**
     * 閸氬﹪鏀㈤弻鎰嚋閻劍鍩涢惃鍕弿闁?Token閿涘牊鏁肩€靛棎鈧線妾烽弶鍐︹偓浣稿灩閸欓攱妞傜拫鍐暏閿涘鈧?
     *
     * @param userId    閻劍鍩?ID
     * @param keepToken 闂団偓鐟曚椒绻氶悾娆戞畱瑜版挸澧?Token閿涘牏鏁ら幋閿嬫拱娴滅儤鏁肩€靛棙妞傛穱婵堟殌閼奉亣闊╂导姘崇樈閿涘绱遍崣顖欒礋 null
     * @return 鐎圭偤妾崥濠囨敘閻ㄥ嫪绱扮拠婵囨殶
     */
    public int revokeAllOfUser(Long userId, String keepToken) {
        if (userId == null) {
            return 0;
        }
        String userTokensKey = RedisKeyConstant.USER_TOKENS + userId;
        Set<String> tokens = stringRedisTemplate.opsForSet().members(userTokensKey);
        if (tokens == null || tokens.isEmpty()) {
            return 0;
        }
        List<String> revoked = new ArrayList<>();
        for (String token : tokens) {
            if (keepToken != null && keepToken.equals(token)) {
                continue;
            }
            revoked.add(token);
        }
        if (revoked.isEmpty()) {
            return 0;
        }
        List<String> tokenKeys = new ArrayList<>(revoked.size());
        for (String token : revoked) {
            tokenKeys.add(RedisKeyConstant.TOKEN + token);
        }
        stringRedisTemplate.delete(tokenKeys);
        stringRedisTemplate.opsForSet().remove(userTokensKey, (Object[]) revoked.toArray(new String[0]));
        return revoked.size();
    }

    /**
     * 瑜版挸澧犵拠閿嬬湴閹煎搫鐢惃?Token閿涘澆uid閿涘鈧?
     */
    public String currentToken() {
        return TokenUtils.resolveCurrentToken();
    }
}
