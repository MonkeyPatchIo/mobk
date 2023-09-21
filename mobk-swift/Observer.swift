//
//  Observer.swift
//

import SwiftUI
import <y>

struct ObserverView<V: View> : View {
    @ObservedObject var reactionObservable: ReactionObservable<V>

    var body: some View {
        self.reactionObservable.view
    }
}

func Observer<V: View>(@ViewBuilder f: @escaping  () -> V) -> some View {
    let reactionObservable = ReactionObservable(f: f)
    return ObserverView(reactionObservable: reactionObservable)
}

class ReactionObservable<V>: ObservableObject {
    @Published var view: V? = nil
    var disposer: ReactionDisposer? = nil

    init(f: @escaping () -> V) {
        disposer = ApiKt.autorun { [weak self] in
            self?.view = f()
        }
    }

    deinit {
        let d = disposer
        disposer = nil
        DispatchQueue.main.async {
            d?.invoke()
        }
    }

}
