package com.mercadosolidario;

import com.mercadosolidario.model.*;
import com.mercadosolidario.model.enums.*;
import com.mercadosolidario.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EntityPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Test
    void testRolePersistence() {
        Role role = new Role();
        role.setName("ADMIN");
        role.setDescription("Administrador completo");

        Role saved = roleRepository.save(role);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("ADMIN");
    }

    @Test
    void testUserPersistence() {
        Role role = new Role();
        role.setName("USER");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setActive(true);
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        User saved = userRepository.save(user);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
    }

    @Test
    void testFamilyPersistence() {
        Family family = new Family();
        family.setResponsibleName("João Silva");
        family.setCpf("123.456.789-00");
        family.setSpouseName("Maria Silva");
        family.setDependents(2);
        family.setHouseholdSize(4);
        family.setTotalIncome(new BigDecimal("1500.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family.setStatus(FamilyStatus.CADASTRADA);

        Family saved = familyRepository.save(family);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCpf()).isEqualTo("123.456.789-00");
        assertThat(saved.getTotalIncome()).isEqualByComparingTo("1500.00");
    }

    @Test
    void testFamilyMemberPersistence() {
        Family family = new Family();
        family.setResponsibleName("João Silva");
        family.setCpf("123.456.789-00");
        family.setHouseholdSize(2);
        family.setTotalIncome(new BigDecimal("1000.00"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);
        family = familyRepository.save(family);

        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setName("Maria Silva");
        member.setCpf("987.654.321-00");
        member.setBirthDate(LocalDate.of(1990, 5, 15));
        member.setRelationship("Cônjuge");

        FamilyMember saved = entityManager.persistAndFlush(member);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Maria Silva");
    }

    @Test
    void testProductPersistence() {
        Product product = new Product();
        product.setName("Arroz");
        product.setCategory("Alimentos");
        product.setUnit("kg");
        product.setCurrentQuantity(100);
        product.setMinimumQuantity(20);
        product.setActive(true);

        Product saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Arroz");
        assertThat(saved.getCurrentQuantity()).isEqualTo(100);
    }

    @Test
    void testStockMovementPersistence() {
        Product product = new Product();
        product.setName("Feijão");
        product.setUnit("kg");
        product.setCurrentQuantity(50);
        product = productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(MovementType.ENTRADA_DOACAO);
        movement.setQuantity(30);
        movement.setReason("Doação de patrocinador");

        StockMovement saved = stockMovementRepository.save(movement);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getQuantity()).isEqualTo(30);
        assertThat(saved.getType()).isEqualTo(MovementType.ENTRADA_DOACAO);
    }

    @Test
    void testDonationPersistence() {
        Donation donation = new Donation();
        donation.setAmount(new BigDecimal("100.00"));
        donation.setDonorName("João Doador");
        donation.setIsAnonymous(false);
        donation.setStatus(DonationStatus.CRIADA);

        Donation saved = donationRepository.save(donation);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getStatus()).isEqualTo(DonationStatus.CRIADA);
    }

    @Test
    void testBigDecimalPrecision() {
        Family family = new Family();
        family.setResponsibleName("Test");
        family.setCpf("111.222.333-44");
        family.setHouseholdSize(1);
        family.setTotalIncome(new BigDecimal("1234.56"));
        family.setRequestReason(RequestReason.BAIXA_RENDA);
        family.setOrigin(FamilyOrigin.CADASTRO_PROPRIO);

        Family saved = familyRepository.save(family);
        assertThat(saved.getTotalIncome()).isEqualByComparingTo("1234.56");
    }

    @Test
    void testEnumPersistence() {
        Family family = new Family();
        family.setResponsibleName("Test");
        family.setCpf("111.222.333-55");
        family.setHouseholdSize(1);
        family.setTotalIncome(new BigDecimal("500.00"));
        family.setRequestReason(RequestReason.DESEMPREGO);
        family.setOrigin(FamilyOrigin.INDICACAO);
        family.setStatus(FamilyStatus.EM_ANALISE);

        Family saved = familyRepository.save(family);
        assertThat(saved.getRequestReason()).isEqualTo(RequestReason.DESEMPREGO);
        assertThat(saved.getOrigin()).isEqualTo(FamilyOrigin.INDICACAO);
        assertThat(saved.getStatus()).isEqualTo(FamilyStatus.EM_ANALISE);
    }
}
