package io.monkeypatch.mobx.core

class Observable<T>(
    initialValue: T,
    context: ReactiveContext = ReactiveContext.main,
    name: String = context.nameFor("Observable")
) : Atom(
    context, name
), ObservableValue<T> {
    override var value: T = initialValue
        get() {
            context.enforceReadPolicy(this)
            reportObserved()
            return field
        }
        set(newValue) {
            context.enforceWritePolicy(this)
            if (newValue != field) {
                field = newValue
                reportChanged()
            }
        }
}
