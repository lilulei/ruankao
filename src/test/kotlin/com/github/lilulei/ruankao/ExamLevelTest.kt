package com.github.lilulei.ruankao

import com.github.lilulei.ruankao.model.ExamLevel
import com.github.lilulei.ruankao.model.ExamType
import com.github.lilulei.ruankao.services.UserIdentityService
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * 考试级别功能测试类
 */
class ExamLevelTest {

    @Test
    fun testExamLevelEnum() {
        // 测试考试级别枚举的基本功能
        assertEquals("软考高级", ExamLevel.SENIOR.displayName)
        assertEquals("软考中级", ExamLevel.INTERMEDIATE.displayName)
        assertEquals("软考初级", ExamLevel.JUNIOR.displayName)
    }

    @Test
    fun testExamTypeToLevelMapping() {
        // 测试考试类型到级别的映射
        val userService = UserIdentityService()
        
        // 测试高级考试类型
        assertEquals(ExamLevel.SENIOR, getLevelForExamType(ExamType.PROJECT_MANAGER))
        assertEquals(ExamLevel.SENIOR, getLevelForExamType(ExamType.SYSTEM_ANALYST))
        assertEquals(ExamLevel.SENIOR, getLevelForExamType(ExamType.SYSTEM_ARCHITECT))
        
        // 测试中级考试类型
        assertEquals(ExamLevel.INTERMEDIATE, getLevelForExamType(ExamType.SOFTWARE_DESIGNER))
        assertEquals(ExamLevel.INTERMEDIATE, getLevelForExamType(ExamType.NETWORK_ENGINEER))
        assertEquals(ExamLevel.INTERMEDIATE, getLevelForExamType(ExamType.DATABASE_ENGINEER))
        
        // 测试初级考试类型
        assertEquals(ExamLevel.JUNIOR, getLevelForExamType(ExamType.PROGRAMMER))
        assertEquals(ExamLevel.JUNIOR, getLevelForExamType(ExamType.NETWORK_ADMINISTRATOR))
        assertEquals(ExamLevel.JUNIOR, getLevelForExamType(ExamType.INFORMATION_PROCESSING_TECHNICIAN))
    }

    @Test
    fun testLevelToExamTypeMapping() {
        // 测试级别到考试类型的映射
        val userService = UserIdentityService()
        
        // 测试高级级别默认考试类型
        assertEquals(ExamType.PROJECT_MANAGER, userService.getDefaultExamTypeForLevel(ExamLevel.SENIOR))
        
        // 测试中级级别默认考试类型
        assertEquals(ExamType.SOFTWARE_DESIGNER, userService.getDefaultExamTypeForLevel(ExamLevel.INTERMEDIATE))
        
        // 测试初级级别默认考试类型
        assertEquals(ExamType.PROGRAMMER, userService.getDefaultExamTypeForLevel(ExamLevel.JUNIOR))
    }

    @Test
    fun testExamTypesForLevel() {
        // 测试获取指定级别的所有考试类型
        val userService = UserIdentityService()
        
        // 测试高级级别考试类型
        val seniorTypes = userService.getExamTypesForLevel(ExamLevel.SENIOR)
        assertTrue(seniorTypes.contains(ExamType.PROJECT_MANAGER))
        assertTrue(seniorTypes.contains(ExamType.SYSTEM_ANALYST))
        assertTrue(seniorTypes.contains(ExamType.SYSTEM_ARCHITECT))
        assertTrue(seniorTypes.contains(ExamType.NETWORK_PLANNER))
        assertTrue(seniorTypes.contains(ExamType.SYSTEM_PLANNING_MANAGER))
        
        // 测试中级级别考试类型
        val intermediateTypes = userService.getExamTypesForLevel(ExamLevel.INTERMEDIATE)
        assertTrue(intermediateTypes.contains(ExamType.SOFTWARE_DESIGNER))
        assertTrue(intermediateTypes.contains(ExamType.NETWORK_ENGINEER))
        assertTrue(intermediateTypes.contains(ExamType.DATABASE_ENGINEER))
        
        // 测试初级级别考试类型
        val juniorTypes = userService.getExamTypesForLevel(ExamLevel.JUNIOR)
        assertTrue(juniorTypes.contains(ExamType.PROGRAMMER))
        assertTrue(juniorTypes.contains(ExamType.NETWORK_ADMINISTRATOR))
        assertTrue(juniorTypes.contains(ExamType.INFORMATION_PROCESSING_TECHNICIAN))
    }

    /**
     * 辅助函数：根据考试类型获取对应的考试级别
     */
    private fun getLevelForExamType(examType: ExamType): ExamLevel {
        return when (examType) {
            // 软考高级
            ExamType.SYSTEM_ANALYST,
            ExamType.SYSTEM_ARCHITECT,
            ExamType.NETWORK_PLANNER,
            ExamType.PROJECT_MANAGER,
            ExamType.SYSTEM_PLANNING_MANAGER -> ExamLevel.SENIOR
            
            // 软考中级
            ExamType.SYSTEM_INTEGRATION_ENGINEER,
            ExamType.NETWORK_ENGINEER,
            ExamType.INFORMATION_SYSTEM_MANAGEMENT_ENGINEER,
            ExamType.SOFTWARE_TESTER,
            ExamType.DATABASE_ENGINEER,
            ExamType.MULTIMEDIA_DESIGNER,
            ExamType.SOFTWARE_DESIGNER,
            ExamType.INFORMATION_SYSTEM_SUPERVISOR,
            ExamType.E_COMMERCE_DESIGNER,
            ExamType.INFORMATION_SECURITY_ENGINEER,
            ExamType.EMBEDDED_SYSTEM_DESIGNER,
            ExamType.SOFTWARE_PROCESS_EVALUATOR,
            ExamType.COMPUTER_AIDED_DESIGNER,
            ExamType.COMPUTER_HARDWARE_ENGINEER,
            ExamType.INFORMATION_TECHNOLOGY_SUPPORT_ENGINEER -> ExamLevel.INTERMEDIATE
            
            // 软考初级
            ExamType.PROGRAMMER,
            ExamType.NETWORK_ADMINISTRATOR,
            ExamType.INFORMATION_PROCESSING_TECHNICIAN,
            ExamType.INFORMATION_SYSTEM_OPERATION_MANAGER,
            ExamType.MULTIMEDIA_APPLICATION_DESIGNER,
            ExamType.E_COMMERCE_TECHNICIAN,
            ExamType.WEB_DESIGNER -> ExamLevel.JUNIOR
        }
    }
}