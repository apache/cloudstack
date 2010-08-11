#ifndef _VNET_SPINLOCK_H_
#define _VNET_SPINLOCK_H_

typedef struct atomic_t {
    unsigned val;
} atomic_t;

int atomic_read(const atomic_t *v);
int atomic_dec_and_test(atomic_t *v);
void atomic_inc(atomic_t *v);
void atomic_set(atomic_t *v, int x);

typedef struct spinlock_t {
    unsigned val;
} spinlock_t;

#define SPIN_LOCK_UNLOCKED ((struct spinlock_t){})

void spin_lock_init(spinlock_t *lock);

unsigned long _spin_lock_irqsave(spinlock_t *lock);
#define spin_lock_irqsave(lock, flags)	flags = _spin_lock_irqsave(lock)
void spin_unlock_irqrestore(spinlock_t *lock, unsigned long flags);

typedef struct rwlock_t{
    unsigned val;
} rwlock_t;

#define RW_LOCK_UNLOCKED ((struct rwlock_t){})

unsigned long _read_lock_irqsave(rwlock_t *lock);
#define read_lock_irqsave(lock, flags)	flags = _read_lock_irqsave(lock)
void read_unlock_irqrestore(rwlock_t *lock, unsigned long flags);

unsigned long _write_lock_irqsave(rwlock_t *lock);
#define write_lock_irqsave(lock, flags)	flags = _write_lock_irqsave(lock)
void write_unlock_irqrestore(rwlock_t *lock, unsigned long flags);

struct semaphore {
    int count;
};

void init_MUTEX(struct semaphore *sem);
void down(struct semaphore *sem);
void up(struct semaphore *sem);

#endif /* ! _VNET_SPINLOCK_H_ */
