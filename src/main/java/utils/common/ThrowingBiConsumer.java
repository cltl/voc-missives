package utils.common;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws AbnormalProcessException;

    static <T,U> BiConsumer<T, U> throwingBiConsumerWrapper(
            ThrowingBiConsumer<T, U> throwingBiConsumer) {

        return (i, j) -> {
            try {
                throwingBiConsumer.accept(i,j);
            } catch (AbnormalProcessException ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}

