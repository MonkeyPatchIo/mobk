//
//  MobKViewModel.swift
//

import SwiftUI
import <y>

@propertyWrapper class VM<S>: ObservableObject where S: Mobk_viewmodelMobkViewModel {
     var wrappedValue: S

    public init(wrappedValue: S) {
        self.wrappedValue =  wrappedValue()
    }

    deinit {
        self.wrappedValue.onCleared()
    }
}

func asStateObject<S>(_ factory: @autoclosure @escaping () -> S) -> StateObject<VM<S>>  where S: Mobk_viewmodelMobkViewModel {
    return StateObject(wrappedValue: VM(wrappedValue: factory()))
}
