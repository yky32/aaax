package com.aaax.server.usecase;


/**
 * @param <ID> - The type of the id.
 * @param <Args> - The type of the arguments.
 * @param <Result> - The type of the result.
 */
public interface ResultUseCase<ID, Args, Result> extends UseCase<ID, Args> {

    default Result executeWithReturn(ID id, Args args) {
        return null;
    }

    default Args execute(ID id) {
        return null;
    }
}