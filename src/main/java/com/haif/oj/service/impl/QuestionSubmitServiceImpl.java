package com.haif.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haif.oj.common.ErrorCode;
import com.haif.oj.constant.CommonConstant;
import com.haif.oj.exception.BusinessException;
import com.haif.oj.judege.JudgeService;
import com.haif.oj.mapper.QuestionSubmitMapper;
import com.haif.oj.judege.codesandbox.model.JudgeInfo;
import com.haif.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.haif.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.haif.oj.model.entity.Question;
import com.haif.oj.model.entity.QuestionSubmit;
import com.haif.oj.model.entity.User;
import com.haif.oj.model.enums.QuestionSubmitLanguageEnum;
import com.haif.oj.model.enums.QuestionSubmitStatusEnum;
import com.haif.oj.model.vo.QuestionSubmitVO;
import com.haif.oj.model.vo.QuestionVO;
import com.haif.oj.model.vo.UserVO;
import com.haif.oj.service.QuestionService;
import com.haif.oj.service.QuestionSubmitService;
import com.haif.oj.service.UserService;
import com.haif.oj.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author zhf
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2023-08-16 01:12:28
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private JudgeService judgeService;

    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 判断编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        Long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(questionSubmitAddRequest.getLanguage());
        JudgeInfo judeInfo = questionSubmitAddRequest.getJudeInfo();
        if (judeInfo != null) {
            questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judeInfo));
        }
        // 添加题目提交信息，设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "");
        }
        // todo 如果提交成功，则把 题目的提交数 +1
        Question serviceById = questionService.getById(questionId);
        serviceById.setSubmitNum(serviceById.getSubmitNum() + 1);
        questionService.updateById(serviceById);
        Long questionSubmitId = questionSubmit.getId();
        // 执行判题服务
        CompletableFuture.runAsync(() -> {
            QuestionSubmit doJudge = judgeService.doJudge(questionSubmitId);
            if (doJudge.getStatus() == 2) {
                // todo 如果运行成功通过，则把题目的通过数 +1
                serviceById.setAcceptedNum(serviceById.getAcceptedNum() + 1);
                questionService.updateById(serviceById);
            }
        });
        return questionSubmitId;
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.like(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏 仅本人和管理员能看见自己（提交 userId 和登陆用户 id 不同）提交的代码
        QuestionVO questionVO = questionService.getQuestionVO2(questionService.getById(questionSubmitVO.getQuestionId()));
        questionSubmitVO.setQuestionVO(questionVO);
        // 关联查询用户信息
        Long userId = questionSubmit.getUserId();
        // 脱敏 既不是管管理员，又不是本人 不能看代码
        if (userId != loginUser.getId() && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        UserVO userVO = userService.getUserVO(loginUser);
        questionSubmitVO.setUserVO(userVO);
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }

        List<QuestionSubmitVO> submitVOList = questionSubmitList.stream()
                .map(map -> getQuestionSubmitVO(map, loginUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(submitVOList);


        // 1. 关联查询用户信息
//        Set<Long> userIdSet = questionSubmitList.stream().map(QuestionSubmit::getUserId).collect(Collectors.toSet());
//        // 找出每个提交题目的 多个用户信息
//        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
//                .collect(Collectors.groupingBy(User::getId));
//        // 填充信息
//        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream().map(questionSubmit -> {
//            QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
//            Long userId = questionSubmit.getUserId();
//            User user = null;
//            if (userIdUserListMap.containsKey(userId)) {
//                user = userIdUserListMap.get(userId).get(0);
//            }
//            questionSubmitVO.setUserVO(userService.getUserVO(user));
//            return questionSubmitVO;
//        }).collect(Collectors.toList());
//        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }


}




