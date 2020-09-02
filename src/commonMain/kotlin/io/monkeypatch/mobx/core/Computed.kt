package io.monkeypatch.mobx.core

class Computed<T>(
    context: ReactiveContext = ReactiveContext.main,
    name: String = context.nameFor("Computed"),
    private val isEqual: EqualityComparer<T?>? = null,
    private val fn: () -> T
) : Atom(
    context, name
), Derivation, ObservableValue<T> {
    override var observables: Set<Atom> = emptySet()
    override var newObservables: MutableSet<Atom> = mutableSetOf()
    override var dependenciesState: DerivationState = DerivationState.NOT_TRACKING
    override var errorValue: MobXException.MobXCaughtException? = null

    private var _value: T? = null
    private var isComputing = false

    override val value: T get() {
        if (isComputing) {
            throw MobXException.MobXCyclicReactionException(
                "Cycle detected in computation $name"
            )
        }

        if (!context.isWithinBatch && observers.isEmpty()) {
            if (context.shouldCompute(this)) {
                context.startBatch()
                _value = computeValue(false)
                context.endBatch()
            }
        } else {
            reportObserved()
            if (context.shouldCompute(this)) {
                if (trackAndCompute()) {
                    context.propagateChangeConfirmed(this)
                }
            }
        }

        if (context.hasCaughtException(this)) {
            throw errorValue!!
        }

        return _value as T
    }

    private fun computeValue(track: Boolean): T? {
        isComputing = true
        context.pushComputation()

        var value: T? = null
        if (track) {
            value = context.trackDerivation(this, fn)
        } else {
            if (context.config.disableErrorBoundaries) {
                value = fn()
            } else {
                try {
                    value = fn()
                    errorValue = null
                } catch (t: Throwable) {
                    errorValue = MobXException.MobXCaughtException(t)
                }
            }
        }

        context.popComputation()
        isComputing = false

        return value
    }

    override fun suspend() {
        context.clearObservables(this)
        _value = null
    }

    override fun onBecomeStale() {
        context.propagatePossibilyChanged(this)
    }

    private fun trackAndCompute(): Boolean {
        //TODO spy

        val oldValue = _value
        val wasSuspended = dependenciesState == DerivationState.NOT_TRACKING
        val newValue = computeValue(true)

        val changed = wasSuspended || context.hasCaughtException(this) ||
            !isEqualHelper(oldValue, newValue)

        if (changed) {
            _value = newValue
        }
        return changed
    }

    private fun isEqualHelper(x: T?, y: T?) =
        isEqual?.let { it(x, y) } ?: x == y


    //TODO observe ?

//     Function observe(void Function(ChangeNotification<T>) handler,
//       {@deprecated bool fireImmediately}) {
//     T prevValue;

//     void notifyChange() {
//       _context.untracked(() {
//         handler(ChangeNotification(
//             type: OperationType.update,
//             object: this,
//             oldValue: prevValue,
//             newValue: value));
//       });
//     }

//     return autorun((_) {
//       final newValue = value;

//       notifyChange();

//       prevValue = newValue;
//     }, context: _context);
//   }


}