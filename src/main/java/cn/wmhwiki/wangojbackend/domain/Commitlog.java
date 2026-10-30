package cn.wmhwiki.wangojbackend.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 提交记录
 *
 * @author WangOJ contributors
 * @TableName commitLog
 */
@TableName(value = "commitLog")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Commitlog implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long problemId;
    private String code;
    private String status;
    private String limitOption;
    private Date createdTime;
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
    private String lang;
    /**
     * 编译错误或系统错误信息（截断后落库，便于用户自纠）
     */
    private String errorMessage;
    /**
     * 逐测试点结果 JSON
     */
    private String caseResult;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Commitlog other = (Commitlog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
                && (this.getProblemId() == null ? other.getProblemId() == null : this.getProblemId().equals(other.getProblemId()))
                && (this.getCode() == null ? other.getCode() == null : this.getCode().equals(other.getCode()))
                && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
                && (this.getLimitOption() == null ? other.getLimitOption() == null : this.getLimitOption().equals(other.getLimitOption()))
                && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
                && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()))
                && (this.getLang() == null ? other.getLang() == null : this.getLang().equals(other.getLang()))
                && (this.getErrorMessage() == null ? other.getErrorMessage() == null
                    : this.getErrorMessage().equals(other.getErrorMessage()))
                && (this.getCaseResult() == null ? other.getCaseResult() == null
                    : this.getCaseResult().equals(other.getCaseResult()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getProblemId() == null) ? 0 : getProblemId().hashCode());
        result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getLimitOption() == null) ? 0 : getLimitOption().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getIsDeleted() == null) ? 0 : getIsDeleted().hashCode());
        result = prime * result + ((getLang() == null) ? 0 : getLang().hashCode());
        result = prime * result + ((getErrorMessage() == null) ? 0 : getErrorMessage().hashCode());
        result = prime * result + ((getCaseResult() == null) ? 0 : getCaseResult().hashCode());
        return result;
    }

    @Override
    public String toString() {
        String sb = getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", id=" + id +
                ", userId=" + userId +
                ", problemId=" + problemId +
                ", code=" + code +
                ", status=" + status +
                ", limitOption=" + limitOption +
                ", createdTime=" + createdTime +
                ", isDeleted=" + isDeleted +
                ", serialVersionUID=" + serialVersionUID +
                "]";
        return sb;
    }
}