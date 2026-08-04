-- ULTARY local test users
-- password: Test1234!  ({noop} = DelegatingPasswordEncoder 평문, 로컬 테스트 전용)
-- 운영 환경에서는 절대 {noop} 사용 금지. 회원가입 구현 시 BCrypt({bcrypt}...)로 저장.

INSERT INTO `ultary_user` (
  `login_id`,
  `password`,
  `name`,
  `nickname`,
  `email`,
  `phone`,
  `bio`,
  `region_sido`,
  `region_sigungu`,
  `withdrawal_status`
) VALUES
(
  'testuser1',
  '{noop}Test1234!',
  '테스트유저1',
  '울타리견주1',
  'testuser1@ultary.local',
  '01011112222',
  '로컬 테스트 계정 1',
  '서울특별시',
  '강남구',
  'ACTIVE'
),
(
  'testuser2',
  '{noop}Test1234!',
  '테스트유저2',
  '울타리냥이2',
  'testuser2@ultary.local',
  '01033334444',
  '로컬 테스트 계정 2',
  '경기도',
  '성남시',
  'ACTIVE'
);
