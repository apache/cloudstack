#include "spinlock.h"

int atomic_read(const atomic_t *v){
    return v->val;
}

int atomic_dec_and_test(atomic_t *v){
    if(v->val > 0){
        v->val--;
        return v->val == 0;
    }
    return 0;
}

void atomic_inc(atomic_t *v){
    v->val++;
}

void atomic_set(atomic_t *v, int x){
    v->val = x;
}

void spin_lock_init(spinlock_t *lock){
    *lock = (spinlock_t){};
}

unsigned long _spin_lock_irqsave(spinlock_t *lock){
    lock->val++;
    return 0;
}

void spin_unlock_irqrestore(spinlock_t *lock, unsigned long flags){
    lock->val--;
}

unsigned long _read_lock_irqsave(rwlock_t *lock){
    lock->val++;
    return 0;
}

void read_unlock_irqrestore(rwlock_t *lock, unsigned long flags){
    lock->val--;
}

unsigned long _write_lock_irqsave(rwlock_t *lock){
    lock->val++;
    return 0;
}

void write_unlock_irqrestore(rwlock_t *lock, unsigned long flags){
    lock->val--;
}

void init_MUTEX(struct semaphore *sem){
    *sem = (struct semaphore){ .count = 1 };
}

void down(struct semaphore *sem){
    sem->count--;
}

void up(struct semaphore *sem){
    sem->count++;
}

