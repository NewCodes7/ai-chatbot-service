package com.chatbot.support

import com.chatbot.domain.rag.service.RagService
import com.chatbot.domain.user.entity.Role
import com.chatbot.domain.user.entity.User
import com.chatbot.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val ragService: RagService,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        seedUsers()
        seedDocumentChunks()
    }

    private fun seedUsers() {
        if (userRepository.count() > 0) return
        log.info("Seeding test users...")
        userRepository.saveAll(
            listOf(
                User(email = "member@test.com", password = passwordEncoder.encode("member1234"), name = "테스트 멤버", role = Role.MEMBER),
                User(email = "admin@test.com", password = passwordEncoder.encode("admin1234"), name = "테스트 관리자", role = Role.ADMIN)
            )
        )
        log.info("Test users created — member@test.com (MEMBER), admin@test.com (ADMIN)")
    }

    private fun seedDocumentChunks() {
        if (ragService.countChunks() > 0) return
        log.info("Seeding document chunks with Gemini embeddings (may take a moment)...")

        val chunks = listOf(
            "코스피(KOSPI)는 한국거래소에 상장된 모든 종목의 시가총액을 기준으로 산출하는 종합주가지수입니다. 1980년 1월 4일을 기준시점(100포인트)으로 하며, 한국 주식시장의 전반적인 흐름을 나타내는 대표 지수입니다." to "kospi_overview",
            "주가수익비율(PER, Price-Earnings Ratio)은 주가를 주당순이익(EPS)으로 나눈 값으로, 기업의 이익 대비 주가 수준을 나타냅니다. PER이 낮을수록 상대적으로 저평가된 것으로 볼 수 있으나, 업종별 특성을 함께 고려해야 합니다." to "per_explanation",
            "분산투자는 다양한 자산이나 종목에 투자금을 나눠 리스크를 줄이는 전략입니다. 상관관계가 낮은 자산들로 포트폴리오를 구성할수록 전체 변동성이 감소합니다. '달걀을 한 바구니에 담지 말라'는 격언이 이 원칙을 잘 설명합니다." to "diversification",
            "채권은 정부, 지방자치단체, 기업 등이 자금 조달을 위해 발행하는 확정이자부 증권입니다. 만기와 이자율이 미리 정해져 있어 주식보다 안정적이지만, 금리 변동에 따라 채권 가격이 반대 방향으로 움직이는 특성이 있습니다." to "bond_basics",
            "ETF(Exchange Traded Fund)는 특정 지수를 추종하며 주식처럼 거래소에서 매매할 수 있는 펀드입니다. 낮은 보수, 높은 분산효과, 실시간 거래 가능 등의 장점으로 개인 투자자들에게 인기가 높습니다. 코스피200 ETF, S&P500 ETF 등이 대표적입니다." to "etf_overview"
        )

        chunks.forEachIndexed { index, (text, source) ->
            try {
                val embedding = ragService.embedText(text)
                ragService.insertChunk(text, embedding, source, index)
                log.info("  Embedded chunk ${index + 1}/${chunks.size}: $source")
            } catch (e: Exception) {
                log.warn("  Failed to embed chunk $source: ${e.message}")
            }
        }
        log.info("Document seeding complete")
    }
}
