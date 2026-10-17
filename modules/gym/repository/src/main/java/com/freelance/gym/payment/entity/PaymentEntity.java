package com.freelance.gym.payment.entity;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "FL_PAYMENTS")
@NamedQueries({
        @NamedQuery(
                name = PaymentEntity.QUERY_FIND_BY_IDS,
                query = "SELECT p FROM PaymentEntity p WHERE p.id IN :ids ORDER BY p.dueDate ASC"
        ),
        @NamedQuery(
                name = PaymentEntity.QUERY_DELETE_BY_IDS,
                query = "DELETE FROM PaymentEntity p WHERE p.id IN :ids"
        ),
        @NamedQuery(
                name = PaymentEntity.QUERY_DELETE_PENDING_BY_IDS,
                query = "DELETE FROM PaymentEntity p WHERE p.id IN :ids AND (p.paidAmount IS NULL OR p.paidAmount = 0)"
        )
})
public class PaymentEntity extends AbstractAuditEntity {

    public static final String QUERY_FIND_BY_IDS = "PaymentEntity.findByIds";
    public static final String QUERY_DELETE_BY_IDS = "PaymentEntity.deleteByIds";
    public static final String QUERY_DELETE_PENDING_BY_IDS = "PaymentEntity.deletePendingByIds";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod = PaymentMethod.NONE;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "notes", length = 500)
    private String notes;

    public PaymentEntity() {
    }

    public PaymentEntity(UUID id, BigDecimal amount, BigDecimal paidAmount, LocalDate dueDate, LocalDate paidDate, PaymentMethod paymentMethod, String transactionId, String notes) {
        this.id = id;
        this.amount = amount;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.dueDate = dueDate;
        this.paidDate = paidDate;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.NONE;
        this.transactionId = transactionId;
        this.notes = notes;
    }

    public PaymentEntity(BigDecimal amount, BigDecimal paidAmount, LocalDate dueDate, LocalDate paidDate, PaymentMethod paymentMethod, String transactionId, String notes) {
        this(UUID.randomUUID(), amount, paidAmount, dueDate, paidDate, paymentMethod, transactionId, notes);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.NONE;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
