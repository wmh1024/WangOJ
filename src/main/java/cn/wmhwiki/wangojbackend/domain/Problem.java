package cn.wmhwiki.wangojbackend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目信息
 *
 * @author WangOJ contributors
 * @TableName problem
 */
@TableName(value = "problem")
@Data
public class Problem implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String tags;
    private String testCase;
    private String limitOption;
    private Integer commitNum;
    private Integer acceptNum;
    private Date createdTime;
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

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
        Problem other = (Problem) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
                && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
                && (this.getContent() == null ? other.getContent() == null : this.getContent().equals(other.getContent()))
                && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()))
                && (this.getTestCase() == null ? other.getTestCase() == null : this.getTestCase().equals(other.getTestCase()))
                && (this.getLimitOption() == null ? other.getLimitOption() == null : this.getLimitOption().equals(other.getLimitOption()))
                && (this.getCommitNum() == null ? other.getCommitNum() == null : this.getCommitNum().equals(other.getCommitNum()))
                && (this.getAcceptNum() == null ? other.getAcceptNum() == null : this.getAcceptNum().equals(other.getAcceptNum()))
                && (this.getCreatedTime() == null ? other.getCreatedTime() == null : this.getCreatedTime().equals(other.getCreatedTime()))
                && (this.getIsDeleted() == null ? other.getIsDeleted() == null : this.getIsDeleted().equals(other.getIsDeleted()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getContent() == null) ? 0 : getContent().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        result = prime * result + ((getTestCase() == null) ? 0 : getTestCase().hashCode());
        result = prime * result + ((getLimitOption() == null) ? 0 : getLimitOption().hashCode());
        result = prime * result + ((getCommitNum() == null) ? 0 : getCommitNum().hashCode());
        result = prime * result + ((getAcceptNum() == null) ? 0 : getAcceptNum().hashCode());
        result = prime * result + ((getCreatedTime() == null) ? 0 : getCreatedTime().hashCode());
        result = prime * result + ((getIsDeleted() == null) ? 0 : getIsDeleted().hashCode());
        return result;
    }

    @Override
    public String toString() {
        String sb = getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", id=" + id +
                ", title=" + title +
                ", content=" + content +
                ", tags=" + tags +
                ", testCase=" + testCase +
                ", limitOption=" + limitOption +
                ", commitNum=" + commitNum +
                ", acceptNum=" + acceptNum +
                ", createdTime=" + createdTime +
                ", isDeleted=" + isDeleted +
                ", serialVersionUID=" + serialVersionUID +
                "]";
        return sb;
    }
}